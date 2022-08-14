package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.net.util.Assert;
import org.net.util.JavaScriptUtils;
import org.net.util.PropertyPlaceholderHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Docker持续部署
 * https://hub.docker.com/
 * <p>
 * 以root用户进入docker容器
 * $ sudo docker exec -it -u root [CONTAINER ID | CONTAINER NAME] /bin/bash
 * <p>
 * $ docker inspect [CONTAINER ID]
 * $ cd [${MergedDir}]
 *
 * @author xiangqian
 * @date 23:49 2022/07/27
 */
@Slf4j
public class JarDockerCd extends AbstractCd {

    //
    private File[] srcFiles;
    private File jarFile;

    // docker
    private DockerBuild dockerBuild;
    private DockerRun dockerRun;

    // script
    private File[] scriptFiles;

    private void initScript() throws Exception {
        log.debug("准备初始化脚本 ...");

        // 校验脚本文件
        String[] scriptPaths = {"cd/docker/jar/Dockerfile", "cd/docker/jar/clear.sh"};
        scriptFiles = checkScript(scriptPaths);

        // 定义以 "${" 开头，以 "}" 结尾的占位符
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put("FILE_NAMES", StringUtils.join(Arrays.stream(srcFiles).map(File::getName).collect(Collectors.toList()), " "));
        placeholderMap.put("JAR_NAME", jarFile.getName());
        placeholderMap.put("ABSOLUTE_WORK_DIR", absoluteWorkDir);
        placeholderMap.put("TAG", dockerBuild.tag());
        placeholderMap.put("NAME", dockerRun.name());

        // 初始化脚本文件
        StringBuilder content = new StringBuilder();
        for (File scriptFile : scriptFiles) {
            content.setLength(0);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(scriptFile));
                String line = null;
                while (Objects.nonNull(line = br.readLine())) {
                    line = propertyPlaceholderHelper.replacePlaceholders(line, placeholderMap::get);
                    if (line.startsWith("JS \"")) {
                        Function<String, String> preFunction = script -> {
                            if (script.endsWith("\\") || script.endsWith("\"")) {
                                script = script.substring(0, script.length() - 1);
                            }
                            return script.replace(" out(", "result.push(");
                        };
                        StringBuilder jsScriptBuilder = new StringBuilder();
                        jsScriptBuilder.append("function execute(){").append('\n');
                        jsScriptBuilder.append('\t').append("var result = [];").append('\n');
                        jsScriptBuilder.append('\t').append(preFunction.apply(line.substring("JS \"".length()))).append('\n');
                        if (!line.endsWith("\"")) {
                            while (Objects.nonNull(line = br.readLine())) {
                                jsScriptBuilder.append(preFunction.apply(line)).append('\n');
                                if (line.endsWith("\"")) {
                                    break;
                                }
                            }
                        }
                        jsScriptBuilder.append('\t').append("return result;").append('\n');
                        jsScriptBuilder.append("};").append('\n');
                        jsScriptBuilder.append("execute();");

                        List<Map<String, Object>> fileList = new ArrayList<>(srcFiles.length);
                        for (File srcFile : srcFiles) {
                            fileList.add(Map.of("name", srcFile.getName(), "isDir", srcFile.isDirectory()));
                        }
                        List<Object> result = JavaScriptUtils.execute(jsScriptBuilder.toString(),
                                Map.of("files", fileList), List.class);
                        content.append(StringUtils.join(result, '\n')).append('\n');
                        continue;
                    }
                    content.append(line).append('\n');
                }
            } finally {
                IOUtils.closeQuietly(br);
            }
            FileUtils.write(scriptFile, content, StandardCharsets.UTF_8);
        }

        log.debug("已初始化脚本!");
    }

    /**
     * 构建镜像
     *
     * @throws Exception
     */
    private void buildImage() throws Exception {
        log.debug("准备构建镜像 ...");
        String cmd = String.format("sudo docker build -t %s .", dockerBuild.tag());
        ssh.execute(cmd, Duration.ofMinutes(30), resultConsumer(cmd));
        log.debug("构建镜像成功!");
    }

    /**
     * 启动镜像
     *
     * @throws Exception
     */
    private void runImage() throws Exception {
        log.debug("准备启动镜像 ...");
        // -d: 后台运行容器，并返回容器ID；
        // --name， 指定容器名字，后续可以通过名字进行容器管理
        // -p: 指定端口映射，格式为：主机(宿主)端口:容器端口
        String cmd = String.format("sudo docker run -d --name %s -p %s -t %s", dockerRun.name(), dockerRun.p(), dockerBuild.tag());
        ssh.execute(cmd, Duration.ofMinutes(30), resultConsumer(cmd));
        log.debug("已启动镜像!");
    }

    @Override
    protected File[] getFilesToBeCompressed() {
        return ListUtils.union(Arrays.stream(srcFiles).collect(Collectors.toList()),
                Arrays.stream(scriptFiles).collect(Collectors.toList())).toArray(File[]::new);
    }

    @Override
    protected File[] getScriptFiles() {
        return new File[]{scriptFiles[1]};
    }

    @Override
    protected void compressBeforePost() throws Exception {
        initScript();
    }

    @Override
    protected void decompressAfterPost() throws Exception {
        buildImage();
        runImage();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, JarDockerCd> {
        private String[] srcFilePaths;
        private DockerBuild dockerBuild;
        private DockerRun dockerRun;

        public Builder() {
            this.dockerBuild = new DockerBuild(this);
            this.dockerRun = new DockerRun(this);
        }

        public Builder srcFilePaths(String... srcFilePaths) {
            this.srcFilePaths = srcFilePaths;
            return this;
        }

        public DockerBuild dockerBuild() {
            return dockerBuild;
        }

        public DockerRun dockerRun() {
            return dockerRun;
        }

        @Override
        protected JarDockerCd get() {
            return new JarDockerCd();
        }

        @Override
        public JarDockerCd build() throws Exception {
            // 校验资源文件路径
            File[] srcFiles = checkSrcFilePaths(srcFilePaths);
            File jarFile = getJarFile(srcFiles);

            // 校验docker参数
            dockerBuild.check();
            dockerRun.check();

            // build
            JarDockerCd jarDockerCd = super.build();
            jarDockerCd.srcFiles = srcFiles;
            jarDockerCd.jarFile = jarFile;
            jarDockerCd.dockerBuild = dockerBuild;
            jarDockerCd.dockerRun = dockerRun;
            return jarDockerCd;
        }
    }

    public static class DockerRun extends Docker {

        // docker run --name="test": 为容器指定一个名称
        private String name;

        // -p: 指定端口映射，格式为：主机(宿主)端口:容器端口
        private String p;

        private DockerRun(Builder builder) {
            super(builder);
        }

        private String name() {
            return name;
        }

        public DockerRun name(String name) {
            this.name = name;
            return this;
        }

        private String p() {
            return p;
        }

        public DockerRun p(String p) {
            this.p = p;
            return this;
        }

        private void check() {
            Assert.notNull(name = StringUtils.trimToNull(name), "name不能为空!");
            Assert.notNull(p = StringUtils.trimToNull(p), "p不能为空!");
        }
    }

    public static class DockerBuild extends Docker {
        // docker build --tag, -t: 镜像的名字及标签，通常 name:tag 或者 name 格式；可以在一次构建中为一个镜像设置多个标签。
        private String tag;

        private DockerBuild(Builder builder) {
            super(builder);
        }

        private String tag() {
            return tag;
        }

        public DockerBuild tag(String tag) {
            this.tag = tag;
            return this;
        }

        private void check() {
            Assert.notNull(tag = StringUtils.trimToNull(tag), "tag不能为空!");
        }
    }

    public static abstract class Docker {
        private Builder builder;

        protected Docker(Builder builder) {
            this.builder = builder;
        }

        public Builder and() {
            return builder;
        }
    }

}

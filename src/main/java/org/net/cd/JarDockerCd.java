package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.net.sftp.FileTransferMode;
import org.net.sftp.impl.DefaultSftpProgressMonitor;
import org.net.util.Assert;
import org.net.util.CompressionUtils;
import org.net.util.JavaScriptUtils;
import org.net.util.PropertyPlaceholderHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
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

    private List<File> files;

    // --tag, -t: 镜像的名字及标签，通常 name:tag 或者 name 格式。
    private String t;
    // -p: 指定端口映射，格式为：主机(宿主)端口:容器端口
    private String p;
    // --name
    private String name;

    // temp dir
    private String tempDirName;
    private File tempDirFile;

    // .tar.gz
    private String tarGzFileName;
    private File tarGzFile;

    // script
    private File dockerfileFile;
    private File clearFile;


    private void clear() throws Exception {
        log.debug("准备清除上一个版本信息 ...");
        String cmd = "./clear.sh";
        ssh.execute(cmd, resultConsumer(cmd));
        log.debug("已清除上一个版本信息!");
    }

    private void initScript() throws Exception {
        log.debug("准备初始化脚本 ...");

        // 校验脚本文件
        String[] scriptPaths = {"cd/docker/jar/Dockerfile", "cd/docker/jar/clear.sh"};
        Consumer<File>[] scriptFileConsumers = new Consumer[]{(Consumer<File>) file -> JarDockerCd.this.dockerfileFile = file,
                (Consumer<File>) file -> JarDockerCd.this.clearFile = file};
        for (int i = 0, length = scriptPaths.length; i < length; i++) {
            String scriptPath = scriptPaths[i];
            URL scriptUrl = this.getClass().getClassLoader().getResource(scriptPath);
            Assert.notNull(scriptUrl, String.format("未找到 %s 文件", scriptPath));
            File scriptFile = new File(scriptUrl.getFile());
            Assert.isTrue(scriptFile.exists(), String.format("%s 文件不存在", scriptPath));
            scriptFileConsumers[i].accept(scriptFile);
        }

        String jarName = null;
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.endsWith(".jar")) {
                jarName = fileName;
                break;
            }
        }
        Assert.notNull(jarName, "无法解析jar名称!");

        // 定义以 "${" 开头，以 "}" 结尾的占位符
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put("FILE_NAMES", StringUtils.join(files.stream().map(File::getName).collect(Collectors.toList()), " "));
        placeholderMap.put("JAR_NAME", jarName);
        placeholderMap.put("ABSOLUTE_WORK_DIR", absoluteWorkDir);
        placeholderMap.put("TAG", t);
        placeholderMap.put("NAME", name);

        // 初始化脚本文件
        StringBuilder content = new StringBuilder();
        File[] scriptFiles = {dockerfileFile, clearFile};
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

                        List<Map<String, Object>> fileList = new ArrayList<>(files.size());
                        for (File file : files) {
                            fileList.add(Map.of("name", file.getName(), "isDir", file.isDirectory()));
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
     * 压缩要上传的文件或文件夹
     */
    private void compress() throws IOException {
        log.debug("准备压缩本地文件或文件夹 ...");

        // 创建临时目录，用于存放文件集
        String tempDirPath = FileUtils.getTempDirectoryPath();
        log.debug("tempDirPath: {}", tempDirPath);
        tempDirName = "temp_" + UUID.randomUUID().toString().replace("-", "");
        tempDirFile = new File(tempDirPath + File.separator + tempDirName);
        log.debug("tempDirFilePath: {}", tempDirFile.getAbsolutePath());

        Assert.isTrue(!tempDirFile.exists(), String.format("%s 已存在此临时文件! (运气真好)", tempDirFile));
        tempDirFile.mkdirs();

        // 将要上传的文件放入到临时目录中
        for (File file : files) {
            // 拷贝文件
            if (file.isFile()) {
                FileUtils.copyFileToDirectory(file, tempDirFile);
            }
            // 拷贝目录
            else {
                FileUtils.copyDirectoryToDirectory(file, tempDirFile);
            }
        }
        FileUtils.copyFileToDirectory(dockerfileFile, tempDirFile);
        FileUtils.copyFileToDirectory(clearFile, tempDirFile);

        // 压缩临时目录
        tarGzFileName = tempDirName + ".tar.gz";
        tarGzFile = new File(tempDirPath + File.separator + tarGzFileName);
        CompressionUtils.tarGz(tempDirFile, tarGzFile);
        log.debug("已压缩本地文件或文件夹! (压缩文件为 {})", tarGzFile.getAbsolutePath());
    }

    /**
     * 上传压缩包
     *
     * @throws Exception
     */
    private void upload() throws Exception {
        log.debug("准备上传压缩文件({}) ...", tarGzFile.getAbsolutePath());
        sftp.put(tarGzFile.getAbsolutePath(), tarGzFileName, DefaultSftpProgressMonitor.builder().build(), FileTransferMode.OVERWRITE);
        log.debug("已上传压缩文件!");
    }

    /**
     * 解压
     *
     * @throws Exception
     */
    private void decompress() throws Exception {
        log.debug("准备解压文件({}) ...", tarGzFileName);
        String cmd = String.format("tar -zxvf ./%s", tarGzFileName);
        ssh.execute(cmd, Duration.ofMinutes(5), resultConsumer(cmd));
        log.debug("已解压文件!");
    }

    private void preprocess() throws Exception {
        log.debug("预处理 ...");

        // 移动临时目录文件集到工作目录（workDir）
        String cmd = String.format("mv ./%s/* ./", tempDirName);
        ssh.execute(cmd, resultConsumer(cmd));

        // 删除临时目录
        cmd = String.format("rm -rf ./%s", tempDirName);
        ssh.execute(cmd, resultConsumer(cmd));

        // 授予 clear.sh 可执行权限
        ssh.execute("chmod +x clear.sh");

        log.debug("预处理完成!");
    }

    /**
     * 构建镜像
     *
     * @throws Exception
     */
    private void buildImage() throws Exception {
        log.debug("准备构建镜像 ...");
        // --tag, -t: 镜像的名字及标签，通常 name:tag 或者 name 格式；可以在一次构建中为一个镜像设置多个标签。
        String cmd = String.format("sudo docker build -t %s .", t);
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
        String cmd = String.format("sudo docker run -d --name %s -p %s -t %s", name, p, t);
        ssh.execute(cmd, Duration.ofMinutes(30), resultConsumer(cmd));
        log.debug("已启动镜像!");
    }

    /**
     * 删除压缩包
     *
     * @throws Exception
     */
    private void deleteArchive() throws Exception {
        log.debug("准备删除压缩文件({}) ...", tarGzFileName);
        String cmd = String.format("rm -rf ./%s", tarGzFileName);
        ssh.execute(cmd, resultConsumer(cmd));
        log.debug("已删除压缩文件!");
    }

    @Override
    public void execute() throws Exception {
        try {
            cdWorkDir();
            initScript();
            clear();
            compress();
            upload();
            decompress();
            preprocess();
            buildImage();
            runImage();
            deleteArchive();
        } finally {
            if (Objects.nonNull(tarGzFile)) {
                FileUtils.forceDelete(tarGzFile);
            }
            if (Objects.nonNull(tempDirFile)) {
                FileUtils.forceDelete(tempDirFile);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, JarDockerCd> {
        private String[] filePaths;
        private String name;
        private String t;
        private String p;

        public Builder filePaths(String... filePaths) {
            this.filePaths = filePaths;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder t(String t) {
            this.t = t;
            return this;
        }

        public Builder p(String p) {
            this.p = p;
            return this;
        }

        @Override
        protected JarDockerCd get() {
            return new JarDockerCd();
        }

        @Override
        public JarDockerCd build() throws Exception {
            Assert.notNull(workDir, "workDir不能为空!");
            Assert.notNull(filePaths, "filePaths不能为空!");
            Assert.notNull(name = StringUtils.trimToNull(name), "--name不能为空!");
            Assert.notNull(t = StringUtils.trimToNull(t), "-t不能为空!");
            Assert.notNull(p = StringUtils.trimToNull(p), "-p不能为空!");

            int length = filePaths.length;
            List<File> files = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                String filePath = filePaths[i];
                Assert.notNull(filePath, String.format("filePath[%s]不能为空!", i));
                File file = new File(filePath);
                Assert.isTrue(file.exists(), String.format("%s 文件或者文件夹不存在!", file));
                files.add(file);
            }

            JarDockerCd jarDockerCd = super.build();
            jarDockerCd.files = files;
            jarDockerCd.name = name;
            jarDockerCd.t = t;
            jarDockerCd.p = p;
            return jarDockerCd;
        }
    }

}

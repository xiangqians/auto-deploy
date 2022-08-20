package org.net.cd.jar;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.net.cd.AbstractCd;
import org.net.cd.Placeholder;
import org.net.util.Assert;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Jar Docker持续部署
 * <p>
 * https://hub.docker.com/
 * <p>
 * 以root用户进入docker容器
 * $ sudo docker exec -it -u root [CONTAINER ID | CONTAINER NAME] /bin/bash
 * <p>
 * $ docker inspect [CONTAINER ID]
 * $ cd [${MergedDir}]
 * <p>
 * Docker安装ping
 * $ apt update && apt install -y iputils-ping
 *
 * @author xiangqian
 * @date 23:49 2022/07/27
 */
@Slf4j
public class JarDockerCd extends AbstractCd {

    // source files
    private File[] sourceFiles;
    private File jarFile;

    // script files
    private File[] scriptFiles;

    // docker
    private DockerBuild dockerBuild;
    private DockerRun dockerRun;

    private JarDockerCd() {
    }

    private JarDockerCd set(DockerBuild dockerBuild, DockerRun dockerRun) {
        this.dockerBuild = dockerBuild;
        this.dockerRun = dockerRun;
        return this;
    }

    @Override
    protected void init() throws Exception {
        // 获取资源文件
        sourceFiles = getSource().get();
        jarFile = getJarFile(sourceFiles);

        // 获取脚本文件
        scriptFiles = getFilesOnClasspath("cd/jar/docker/Dockerfile", "cd/jar/docker/clear.sh");
    }

    @Override
    protected void beforePost() throws Exception {
        log.debug("准备初始化脚本 ...");

        // 占位符参数
        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put(Placeholder.FILE_NAMES, StringUtils.join(Arrays.stream(sourceFiles).map(File::getName).collect(Collectors.toList()), " "));
        placeholderMap.put(Placeholder.JAR_NAME, jarFile.getName());
        placeholderMap.put(Placeholder.ABSOLUTE_WORK_DIR, getAbsoluteWorkDir());
        placeholderMap.put(Placeholder.TAG, dockerBuild.tag);
        placeholderMap.put(Placeholder.NAME, dockerRun.name());
        placeholderMap.put(Placeholder.FILES, getFilesPlaceholderValue(getFilesToBeCompressed()));
        placeholderMap.put(Placeholder.DOCKER_FILES, getFilesPlaceholderValue(sourceFiles));

        // 替换占位符
        replacePlaceholders(scriptFiles, placeholderMap);

        log.debug("已初始化脚本!");
    }

    @Override
    protected File[] getFilesToBeCompressed() {
        return ListUtils.union(Arrays.stream(sourceFiles).collect(Collectors.toList()),
                        Arrays.stream(scriptFiles).collect(Collectors.toList()))
                .toArray(File[]::new);
    }

    @Override
    protected void afterPost() throws Exception {
        chmodX(scriptFiles[1]); // clear.sh
        buildImage();
        runImage();
    }

    /**
     * 构建镜像
     *
     * @throws Exception
     */
    private void buildImage() throws Exception {
        log.debug("准备构建镜像 ...");

        // docker build -f Dockerfile -t org/test:2022.8 .

        List<String> cmds = new ArrayList<>();

        // sudo
        cmds.add("sudo");

        // docker build
        // 命令用于使用 Dockerfile 创建镜像。
        cmds.add("docker build");

        // docker build --tag, -t
        // 镜像的名字及标签，通常 name:tag 或者 name 格式；可以在一次构建中为一个镜像设置多个标签。
        // 例如：org/test:2022.8
        cmds.add(String.format("-t %s .", dockerBuild.tag));

        String cmd = StringUtils.join(cmds, " ");
        executeCmd(cmd, Duration.ofMinutes(30));
        log.debug("构建镜像成功!");
    }

    /**
     * 启动镜像
     *
     * @throws Exception
     */
    private void runImage() throws Exception {
        log.debug("准备启动镜像 ...");

        List<String> cmds = new ArrayList<>();

        // sudo
        cmds.add("sudo");

        // https://docs.docker.com/engine/reference/commandline/run/

        // docker run
        // 创建一个新的容器并运行一个命令
        cmds.add("docker run");

        // -i: 以交互模式运行容器，通常与 -t 同时使用；
        // -d: 后台运行容器，并返回容器ID；
        cmds.add("-id");

        // --name [name]
        // 为容器指定一个名称，后续可以通过名字进行容器管理
        cmds.add(String.format("--name %s", dockerRun.name()));

        // -p [host port]:[container port]
        // 指定端口映射，格式为：主机(宿主)端口:容器端口
        List<String> ps = dockerRun.ps;
        if (CollectionUtils.isNotEmpty(ps)) {
            for (String p : ps) {
                cmds.add(String.format("-p %s", p));
            }
        }

        // -h [hostname]
        // 指定容器的hostname；
        List<String> hs = dockerRun.hs;
        if (CollectionUtils.isNotEmpty(hs)) {
            for (String h : hs) {
                cmds.add(String.format("-h %s", h));
            }
        }

        // --link=[]
        // 添加链接到另一个容器；
        // 例如：--link redis
        List<String> links = dockerRun.links;
        if (CollectionUtils.isNotEmpty(links)) {
            for (String link : links) {
                cmds.add(String.format("--link %s", link));
            }
        }

        // --add-host [hostname]:[ip]
        // --add-host list           Add a custom host-to-IP mapping (host:ip)
        // --add-host db:10.194.188.183
        List<String> add_hosts = dockerRun.add_hosts;
        if (CollectionUtils.isNotEmpty(add_hosts)) {
            for (String add_host : add_hosts) {
                cmds.add(String.format("--add-host %s", add_host));
            }
        }

        // -t: 为容器重新分配一个伪输入终端，通常与 -i 同时使用；
        cmds.add(String.format("-t %s", dockerBuild.tag));

        String cmd = StringUtils.join(cmds, " ");
        executeCmd(cmd, Duration.ofMinutes(30));
        log.debug("已启动镜像!");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, JarDockerCd> {
        private DockerBuild dockerBuild;
        private DockerRun dockerRun;

        public Builder() {
            this.dockerBuild = new DockerBuild(this);
            this.dockerRun = new DockerRun(this);
        }

        public DockerBuild dockerBuild() {
            return dockerBuild;
        }

        public DockerRun dockerRun() {
            return dockerRun;
        }

        @Override
        public JarDockerCd build() throws Exception {
            // 校验docker参数
            dockerBuild.check();
            dockerRun.check();

            // build
            return super.build().set(dockerBuild, dockerRun);
        }
    }

    public static class DockerRun extends Docker {
        private String name;
        private List<String> ps;
        private List<String> hs;
        private List<String> links;
        private List<String> add_hosts;

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

        public DockerRun p(String p) {
            if (Objects.isNull(ps)) {
                ps = new ArrayList<>();
            }
            this.ps.add(p);
            return this;
        }

        public DockerRun h(String h) {
            if (Objects.isNull(hs)) {
                hs = new ArrayList<>();
            }
            this.hs.add(h);
            return this;
        }

        public DockerRun link(String link) {
            if (Objects.isNull(links)) {
                links = new ArrayList<>();
            }
            this.links.add(link);
            return this;
        }

        public DockerRun add_host(String add_host) {
            if (Objects.isNull(add_hosts)) {
                add_hosts = new ArrayList<>();
            }
            this.add_hosts.add(add_host);
            return this;
        }

        private void check() {
            Assert.notNull(name = StringUtils.trimToNull(name), "name不能为空!");
        }
    }

    public static class DockerBuild extends Docker {
        private String tag;

        private DockerBuild(Builder builder) {
            super(builder);
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

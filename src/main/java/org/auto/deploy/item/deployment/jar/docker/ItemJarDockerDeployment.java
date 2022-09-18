package org.auto.deploy.item.deployment.jar.docker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.item.deployment.jar.ItemJarDeployment;
import org.auto.deploy.item.server.ItemServer;
import org.auto.deploy.item.source.ItemSource;
import org.auto.deploy.util.Assert;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jar Docker部署
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
 * @date 20:25 2022/09/10
 */
@Slf4j
public class ItemJarDockerDeployment extends ItemJarDeployment {

    // --name
    private String name;
    // --tag, -t
    private String tag;

    public ItemJarDockerDeployment(Config config, ItemServer server, ItemSource source) {
        super(config, server, source);
    }

    @Override
    protected void afterPost() throws Exception {
        // chmod +x
        chmodX(scriptFiles);

        log.debug("构建镜像 ...");
        // docker build 命令用于使用Dockerfile创建镜像。
        // docker build -f Dockerfile -t org/auto-deploy:2022.7 .
        List<String> buildCmd = List.of("docker build",
                // docker build --tag, -t
                // 镜像的名字及标签，通常 name:tag 或者 name 格式；可以在一次构建中为一个镜像设置多个标签。
                // 例如：org/auto-deploy:2022.7
                String.format("-t %s", tag),
                "."
        );
        server.executeCmd(StringUtils.join(buildCmd, " "), Duration.ofMinutes(30));
        log.debug("构建镜像成功!");

        log.debug("启动镜像 ...");
        server.executeCmd(StringUtils.join(((Config) config).getRunCmd(), " "), Duration.ofMinutes(30));
        log.debug("已启动镜像!");

        log.debug("获取容器元数据 ...");
        List<String> results = server.executeCmdForResults(String.format("docker inspect %s", name));
        log.debug("\n{}", StringUtils.join(results, "\n"));
        Pattern pattern = Pattern.compile("\"MergedDir\":\\s+\"([^\"]*)");
        Matcher matcher = pattern.matcher(StringUtils.join(results, ""));
        while (matcher.find()) {
            log.debug("MergedDir: {}", matcher.group(1));
        }
        log.debug("已获取容器元数据!");
    }

    @Override
    protected void initScriptFiles() throws Exception {
        // script files
        URL[] scriptUrls = new URL[]{getScriptResource("jar/docker/Dockerfile"),
                getScriptResource("jar/docker/clean.sh")};

        // placeholderMap
        // --name
        // -t
        name = null;
        tag = null;
        List<String> runCmd = ((Config) config).getRunCmd();
        for (String str : runCmd) {
            if (str.startsWith("--name ")) {
                name = str.split(" ")[1];
            } else if (str.startsWith("-t ")) {
                tag = str.split(" ")[1];
            }
        }
        // FILES
        FilesPlaceholderValue filesPlaceholderValue = new FilesPlaceholderValue();
        filesPlaceholderValue.add(pkgFile);
        Optional.ofNullable(addlFiles).ifPresent(files -> Arrays.stream(files).forEach(filesPlaceholderValue::add));
        Arrays.stream(scriptUrls).forEach(filesPlaceholderValue::add);
        // DOCKER_FILES
        FilesPlaceholderValue dockerFilesPlaceholderValue = new FilesPlaceholderValue();
        dockerFilesPlaceholderValue.add(pkgFile);
        Optional.ofNullable(addlFiles).ifPresent(files -> Arrays.stream(files).forEach(dockerFilesPlaceholderValue::add));
        Map<String, Object> placeholderMap = Map.of("NAME", name,
                "TAG", tag,
                "FILES", filesPlaceholderValue,
                "DOCKER_FILES", dockerFilesPlaceholderValue,
                "JAR_NAME", pkgFile.getName());

        // replaceFilePlaceholders
        int length = scriptUrls.length;
        scriptFiles = new File[length];
        for (int i = 0; i < length; i++) {
            scriptFiles[i] = replaceScriptResourcePlaceholders(scriptUrls[i], placeholderMap);
        }
    }

    @Data
    @ToString(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Config extends ItemJarDeployment.Config {

        /**
         * 执行docker超时时间s
         */
        private Integer timeout;

        /**
         * docker命令
         */
        @JsonProperty("run-cmd")
        private List<String> runCmd;

        public void validate() {
            Assert.notNull(getPkgFile(), "deployment.jar-docker.pkg-file不能为null");
            Assert.isTrue(Objects.nonNull(timeout) && timeout > 0, "deployment.jar-docker.timeout必须大于0");
            Assert.notNull(runCmd, "deployment.jar-docker.run-cmd不能为null");
            if (!runCmd.stream().filter(str -> str.startsWith("--name ")).findFirst().map(str -> true).orElse(false)) {
                throw new IllegalArgumentException("deployment.jar-docker.run-cmd必须指定--name");
            }
            if (!runCmd.stream().filter(str -> str.startsWith("-t ")).findFirst().map(str -> true).orElse(false)) {
                throw new IllegalArgumentException("deployment.jar-docker.run-cmd必须指定-t");
            }
        }

    }

}

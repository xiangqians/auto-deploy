package org.auto.deploy.support.deployment;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.config.deployment.JarDockerDeploymentConfig;
import org.auto.deploy.support.Server;
import org.auto.deploy.support.source.Source;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class JarDockerDeployment extends JarDeployment {

    // --name
    private String name;
    // --tag, -t
    private String tag;

    public JarDockerDeployment(JarDockerDeploymentConfig config, Server server, Source source) {
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
        server.executeCmd(StringUtils.join(((JarDockerDeploymentConfig) config).getRunCmd(), " "), Duration.ofMinutes(30));
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
        scriptFiles = new File[]{getScriptFile("jar-docker/Dockerfile"),
                getScriptFile("jar-docker/clean.sh")};

        // placeholderMap
        // --name
        // -t
        name = null;
        tag = null;
        List<String> runCmd = ((JarDockerDeploymentConfig) config).getRunCmd();
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
        Optional.ofNullable(scriptFiles).ifPresent(files -> Arrays.stream(files).forEach(filesPlaceholderValue::add));
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
        for (int i = 0, length = scriptFiles.length; i < length; i++) {
            scriptFiles[i] = replaceFilePlaceholders(scriptFiles[i], placeholderMap);
        }
    }

}

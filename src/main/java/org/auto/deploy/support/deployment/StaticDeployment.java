package org.auto.deploy.support.deployment;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.config.deployment.StaticDeploymentConfig;
import org.auto.deploy.support.Server;
import org.auto.deploy.support.source.Source;
import org.auto.deploy.util.Assert;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author xiangqian
 * @date 23:54 2022/09/10
 */
@Slf4j
public class StaticDeployment extends AbstractDeployment {

    private StaticDeploymentConfig config;
    private Source source;

    // 部署位置绝对路径
    private String absoluteLocation;

    // static打包文件，可能是一个集合（当有通配符时，如：./desc/*）
    private File[] pkgFiles;

    // addl files
    private File[] addlFiles;

    // script file
    private File[] scriptFiles;

    // files
    private File[] files;

    public StaticDeployment(StaticDeploymentConfig config, Server server, Source source) {
        super(server);
        this.config = config;
        this.source = source;
    }

    @Override
    protected void afterPost() throws Exception {
        // chmod +x
        chmodX(scriptFiles);

        // cp -r -f
        Consumer<File> fileConsumer = file -> {
            try {
                server.executeCmd(String.format("cp -r -f ./%s %s", file.getName(), absoluteLocation), false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Arrays.stream(pkgFiles).forEach(fileConsumer);
        Arrays.stream(addlFiles).forEach(fileConsumer);
    }

    @Override
    protected File[] getFiles() {
        return files;
    }

    @Override
    protected void init() throws Exception {
        log.debug("initializing ...");

        // location
        // pwd
        String cmd = String.format("cd %s && pwd", config.getLocation());
        List<String> results = server.executeCmdForResults(cmd);
        log.debug("<ssh> {}\n{}", cmd, StringUtils.join(results, "\n"));
        if (CollectionUtils.isNotEmpty(results)) {
            for (String result : results) {
                if (result.startsWith("/")) {
                    absoluteLocation = result;
                    break;
                }
            }
        }
        Assert.notNull(absoluteLocation, "无法解析部署位置绝对路径");
        log.debug("absoluteLocation: {}", absoluteLocation);
        // 再回到工作目录
        server.executeCmd(String.format("cd %s", server.getAbsoluteWorkDir()), false);

        // pkg file
        File pkgFile = null;
        String pkgFileLocation = config.getPkgFile();
        boolean more = false;
        if (pkgFileLocation.endsWith("/*") || pkgFileLocation.endsWith("\\*")) {
            more = true;
            pkgFile = Path.of(source.get().getAbsolutePath(), pkgFileLocation.substring(0, pkgFileLocation.length() - 2)).toFile();
        } else {
            pkgFile = Path.of(source.get().getAbsolutePath(), pkgFileLocation).toFile();
        }
        Assert.isTrue(Objects.nonNull(pkgFile) && pkgFile.exists(), String.format("pkgFile不存在: %s", pkgFile.getAbsolutePath()));
        pkgFiles = more ? (pkgFile.isFile() ? new File[]{pkgFile} : pkgFile.listFiles()) : new File[]{pkgFile};

        // addl files
        if (CollectionUtils.isNotEmpty(config.getAddlFiles())) {
            String basePath = source.get().getAbsolutePath();
            addlFiles = config.getAddlFiles().stream()
                    .map(name -> getAddlFile(basePath, name))
                    .toArray(File[]::new);
        }

        // script files
        URL[] scriptUrls = new URL[]{getScriptResource("static/clean.sh")};
        // placeholderMap
        FilesPlaceholderValue filesPlaceholderValue = new FilesPlaceholderValue();
        Arrays.stream(pkgFiles).forEach(filesPlaceholderValue::add);
        Optional.ofNullable(addlFiles).ifPresent(files -> Arrays.stream(files).forEach(filesPlaceholderValue::add));
        Arrays.stream(scriptUrls).forEach(filesPlaceholderValue::add);
        // --- location ---
        Arrays.stream(pkgFiles).forEach(file -> filesPlaceholderValue.add(String.format("%s/%s", absoluteLocation, file.getName()), file.isDirectory()));
        Optional.ofNullable(addlFiles).ifPresent(files -> Arrays.stream(files).forEach(file -> filesPlaceholderValue.add(String.format("%s/%s", absoluteLocation, file.getName()), file.isDirectory())));
        Map<String, Object> placeholderMap = Map.of("FILES", filesPlaceholderValue);
        // replaceFilePlaceholders
        int length = scriptUrls.length;
        scriptFiles = new File[length];
        for (int i = 0; i < length; i++) {
            scriptFiles[i] = replaceScriptResourcePlaceholders(scriptUrls[i], placeholderMap);
        }

        // files
        files = ArrayUtils.addAll(pkgFiles, scriptFiles);
        if (Objects.nonNull(addlFiles)) {
            files = ArrayUtils.addAll(files, addlFiles);
        }

        log.debug("initialized!");
    }

}

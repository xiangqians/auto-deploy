package org.auto.deploy.support.deployment;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.auto.deploy.config.deployment.JarDeploymentConfig;
import org.auto.deploy.support.Server;
import org.auto.deploy.support.source.Source;
import org.auto.deploy.util.Assert;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author xiangqian
 * @date 14:44 2022/09/10
 */
@Slf4j
public class JarDeployment extends AbstractDeployment {

    protected JarDeploymentConfig config;
    protected Source source;

    // jar file
    protected File pkgFile;

    // addl files
    protected File[] addlFiles;

    // script file
    protected File[] scriptFiles;

    // files
    protected File[] files;

    public JarDeployment(JarDeploymentConfig config, Server server, Source source) {
        super(server);
        this.config = config;
        this.source = source;
    }

    @Override
    protected void afterPost() throws Exception {
        // chmod +x
        chmodX(scriptFiles);

        log.debug("启动java应用 ...\n\t{}", pkgFile.getName());
        server.executeCmd("./startup.sh", false);
        log.debug("已启动java应用!\n\t{}", pkgFile.getName());
    }

    @Override
    protected File[] getFiles() {
        return files;
    }

    @Override
    protected void init() throws Exception {
        log.debug("initializing ...");

        // init pkg file
        initPkgFile();

        // init addl files
        initAddlFiles();

        // init script files
        initScriptFiles();

        // init files
        initFiles();

        log.debug("initialized!");
    }

    protected void initFiles() {
        files = ArrayUtils.addAll(scriptFiles, pkgFile);
        if (Objects.nonNull(addlFiles)) {
            files = ArrayUtils.addAll(files, addlFiles);
        }
    }

    protected void initScriptFiles() throws Exception {
        // script files
        scriptFiles = new File[]{getScriptFile("jar/jps.sh"),
                getScriptFile("jar/startup.sh"),
                getScriptFile("jar/shutdown.sh"),
                getScriptFile("jar/clean.sh")};

        // placeholderMap
        FilesPlaceholderValue filesPlaceholderValue = new FilesPlaceholderValue();
        filesPlaceholderValue.add(pkgFile);
        Optional.ofNullable(addlFiles).ifPresent(files -> Arrays.stream(files).forEach(filesPlaceholderValue::add));
        Optional.ofNullable(scriptFiles).ifPresent(files -> Arrays.stream(files).forEach(filesPlaceholderValue::add));
        Map<String, Object> placeholderMap = Map.of("FILES", filesPlaceholderValue,
                "JAVA_HOME", config.getJavaHome(),
                "JAR_PATH", String.format("%s/%s", server.getAbsoluteWorkDir(), pkgFile.getName()),
                "JAR_NAME", pkgFile.getName());

        // replaceFilePlaceholders
        for (int i = 0, length = scriptFiles.length; i < length; i++) {
            scriptFiles[i] = replaceFilePlaceholders(scriptFiles[i], placeholderMap);
        }
    }

    protected void initAddlFiles() throws Exception {
        if (CollectionUtils.isNotEmpty(config.getAddlFiles())) {
            String basePath = source.get().getAbsolutePath();
            addlFiles = config.getAddlFiles().stream()
                    .map(name -> getAddlFile(basePath, name))
                    .toArray(File[]::new);
        }
    }

    protected void initPkgFile() throws Exception {
        String pkgFileLocation = config.getPkgFile();
        pkgFile = Path.of(source.get().getAbsolutePath(), pkgFileLocation).toFile();
        Assert.isTrue(Objects.nonNull(pkgFile) && pkgFile.exists(), String.format("pkgFile不存在: %s", pkgFile.getAbsolutePath()));
    }

}

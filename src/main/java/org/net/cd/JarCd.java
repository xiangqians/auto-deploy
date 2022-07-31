package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.net.sftp.FileTransferMode;
import org.net.sftp.impl.DefaultSftpProgressMonitor;
import org.net.util.Assert;
import org.net.util.PropertyPlaceholderHelper;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Jar持续部署
 *
 * @author xiangqian
 * @date 22:30 2022/07/25
 */
@Slf4j
public class JarCd extends AbstractCd {

    private String javaHome;
    private File jarFile;
    private String jarName;

    private File jpsScriptFile;
    private File startupScriptFile;
    private File shutdownScriptFile;

    private JarCd() {
    }

    private void initScript() throws Exception {
        // 校验脚本文件
        String[] scriptPaths = {"cd/jar/jps.sh", "cd/jar/startup.sh", "cd/jar/shutdown.sh"};
        Consumer<File>[] scriptFileConsumers = new Consumer[]{(Consumer<File>) file -> JarCd.this.jpsScriptFile = file,
                (Consumer<File>) file -> JarCd.this.startupScriptFile = file,
                (Consumer<File>) file -> JarCd.this.shutdownScriptFile = file};
        for (int i = 0, length = scriptPaths.length; i < length; i++) {
            String scriptPath = scriptPaths[i];
            URL scriptUrl = this.getClass().getClassLoader().getResource(scriptPath);
            Assert.notNull(scriptUrl, String.format("未找到 %s 文件", scriptPath));
            File scriptFile = new File(scriptUrl.getFile());
            Assert.isTrue(scriptFile.exists(), String.format("%s 文件不存在", scriptPath));
            scriptFileConsumers[i].accept(scriptFile);
        }

        // 定义以 "${" 开头，以 "}" 结尾的占位符
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put("JAVA_HOME", javaHome);
        placeholderMap.put("JAR_PATH", String.format("%s/%s", absoluteWorkDir, jarName));
        placeholderMap.put("JAR_NAME", jarName);

        // 初始化脚本文件
        String content = null;
        File[] scriptFiles = {jpsScriptFile, startupScriptFile, shutdownScriptFile};
        for (File scriptFile : scriptFiles) {
            content = FileUtils.readFileToString(scriptFile, StandardCharsets.UTF_8);
            content = propertyPlaceholderHelper.replacePlaceholders(content, placeholderMap::get);
            FileUtils.write(scriptFile, content, StandardCharsets.UTF_8);
        }
    }

    public void uploadScript() throws Exception {
        // jps.sh
        sftp.put(jpsScriptFile.getAbsolutePath(), "jps.sh", DefaultSftpProgressMonitor.builder().build(), FileTransferMode.OVERWRITE);
        ssh.execute("chmod +x jps.sh");

        // startup.sh
        sftp.put(startupScriptFile.getAbsolutePath(), "startup.sh", DefaultSftpProgressMonitor.builder().build(), FileTransferMode.OVERWRITE);
        ssh.execute("chmod +x startup.sh");

        // shutdown.sh
        sftp.put(shutdownScriptFile.getAbsolutePath(), "shutdown.sh", DefaultSftpProgressMonitor.builder().build(), FileTransferMode.OVERWRITE);
        ssh.execute("chmod +x shutdown.sh");
    }

    public void shutdown() throws Exception {
        String cmd = "./shutdown.sh";
        ssh.execute(cmd, resultConsumer(cmd));
    }

    public void deleteJar() throws Exception {
        String cmd = String.format("rm -rf ./logs ./%s", jarName);
        ssh.execute(cmd, resultConsumer(cmd));
    }

    public void uploadJar() throws Exception {
        sftp.put(jarFile.getAbsolutePath(), jarName, DefaultSftpProgressMonitor.builder().build(), FileTransferMode.OVERWRITE);
    }

    public void startup() throws Exception {
        String cmd = "./startup.sh";
        ssh.execute(cmd, resultConsumer(cmd));
    }

    public void deleteScript() throws Exception {
        String[] paths = {"./startup.sh", "./shutdown.sh", "./jps.sh"};
        for (String path : paths) {
            sftp.rm(path);
            log.debug("rm {}", path);
        }
    }

    @Override
    public void execute() throws Exception {
        cdWorkDir();
        initScript();
        uploadScript();
        shutdown();
        deleteJar();
        uploadJar();
        startup();
        deleteScript();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, JarCd> {
        private String javaHome;
        private String jarFilePath;

        private Builder() {
        }

        public Builder javaHome(String javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        public Builder jarFilePath(String jarFilePath) {
            this.jarFilePath = jarFilePath;
            return this;
        }

        @Override
        protected JarCd get() {
            return new JarCd();
        }

        @Override
        public JarCd build() throws Exception {
            Assert.notNull(javaHome, "javaHome不能为空");
            Assert.notNull(jarFilePath, "jarFilePath不能为空");
            File jarFile = new File(jarFilePath);
            Assert.isTrue(jarFile.exists(), String.format("%s jar文件不存在", jarFilePath));
            Assert.isTrue(!jarFile.isDirectory(), String.format("%s 不能是目录，必须是文件", jarFilePath));
            log.debug("jarFilePath: {}", jarFilePath);

            JarCd jarCd = super.build();
            jarCd.javaHome = javaHome;
            log.debug("javaHome: {}", javaHome);
            jarCd.jarFile = jarFile;
            int index = jarFilePath.lastIndexOf(File.separator);
            String jarName = jarFilePath.substring(index + 1);
            jarCd.jarName = jarName;
            log.debug("jarName: {}", jarName);
            return jarCd;
        }

    }

}

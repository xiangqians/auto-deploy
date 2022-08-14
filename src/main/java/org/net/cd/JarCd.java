package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.net.util.Assert;
import org.net.util.PropertyPlaceholderHelper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Jar持续部署
 *
 * @author xiangqian
 * @date 22:30 2022/07/25
 */
@Slf4j
public class JarCd extends AbstractCd {

    //
    private String javaHome;
    private File[] srcFiles;
    private File jarFile;

    // script
    private File[] scriptFiles;

    private JarCd() {
    }

    private void initScript() throws Exception {
        log.debug("准备初始化脚本 ...");
        // 校验脚本文件
        String[] scriptPaths = {"cd/jar/jps.sh", "cd/jar/startup.sh", "cd/jar/shutdown.sh", "cd/jar/clear.sh"};
        scriptFiles = checkScript(scriptPaths);

        // 定义以 "${" 开头，以 "}" 结尾的占位符
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put("ABSOLUTE_WORK_DIR", absoluteWorkDir);
        placeholderMap.put("JAVA_HOME", javaHome);
        placeholderMap.put("JAR_PATH", String.format("%s/%s", absoluteWorkDir, jarFile.getName()));
        placeholderMap.put("JAR_NAME", jarFile.getName());

        // 初始化脚本文件
        String content = null;
        for (File scriptFile : scriptFiles) {
            content = FileUtils.readFileToString(scriptFile, StandardCharsets.UTF_8);
            content = propertyPlaceholderHelper.replacePlaceholders(content, placeholderMap::get);
            FileUtils.write(scriptFile, content, StandardCharsets.UTF_8);
        }

        log.debug("已初始化脚本!");
    }

    private void startup() throws Exception {
        log.debug("准备启动java应用({}) ...", jarFile.getName());
        String cmd = "./startup.sh";
        ssh.execute(cmd, resultConsumer(cmd));
        log.debug("已启动java应用!");
    }

    @Override
    protected File[] getFilesToBeCompressed() {
        return ListUtils.union(Arrays.stream(srcFiles).collect(Collectors.toList()),
                Arrays.stream(scriptFiles).collect(Collectors.toList())).toArray(File[]::new);
    }

    @Override
    protected File[] getScriptFiles() {
        return scriptFiles;
    }

    @Override
    protected void compressBeforePost() throws Exception {
        initScript();
    }

    @Override
    protected void decompressAfterPost() throws Exception {
        startup();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, JarCd> {
        private String javaHome;
        private String[] srcFilePaths;

        private Builder() {
        }

        public Builder javaHome(String javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        public Builder srcFilePaths(String... srcFilePaths) {
            this.srcFilePaths = srcFilePaths;
            return this;
        }

        @Override
        protected JarCd get() {
            return new JarCd();
        }

        @Override
        public JarCd build() throws Exception {
            //
            Assert.notNull(javaHome, "javaHome不能为空");

            // 校验资源文件路径
            File[] srcFiles = checkSrcFilePaths(srcFilePaths);
            File jarFile = getJarFile(srcFiles);

            JarCd jarCd = super.build();
            jarCd.javaHome = javaHome;
            log.debug("javaHome: {}", javaHome);
            jarCd.srcFiles = srcFiles;
            log.debug("srcFiles: {}", Arrays.stream(srcFiles).map(File::getAbsolutePath).collect(Collectors.toList()));
            jarCd.jarFile = jarFile;
            log.debug("jarFile: {}", jarFile.getAbsolutePath());
            return jarCd;
        }
    }

}

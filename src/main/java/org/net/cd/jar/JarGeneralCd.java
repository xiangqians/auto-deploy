package org.net.cd.jar;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.net.cd.AbstractCd;
import org.net.cd.Placeholder;
import org.net.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Jar通常持续部署
 *
 * @author xiangqian
 * @date 22:30 2022/07/25
 */
@Slf4j
public class JarGeneralCd extends AbstractCd {

    // java home
    private String javaHome;

    // source
    private File[] sourceFiles;
    private File jarFile;

    // script
    private File[] scriptFiles;

    private JarGeneralCd() {
    }

    private JarGeneralCd set(String javaHome) {
        this.javaHome = javaHome;
        return this;
    }

    @Override
    protected void init() throws Exception {
        // 获取资源文件
        sourceFiles = getSource().get();
        log.debug("sourceFiles: {}", Arrays.stream(sourceFiles).map(File::getAbsolutePath).collect(Collectors.toList()));
        jarFile = getJarFile(sourceFiles);
        log.debug("jarFile: {}", jarFile.getAbsolutePath());

        // 获取脚本文件
        scriptFiles = getFilesOnClasspath("cd/jar/general/jps.sh",
                "cd/jar/general/startup.sh",
                "cd/jar/general/shutdown.sh",
                "cd/jar/general/clear.sh");
    }

    @Override
    protected void beforePost() throws Exception {
        log.debug("准备初始化脚本 ...");

        // 占位符参数
        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put(Placeholder.ABSOLUTE_WORK_DIR, getAbsoluteWorkDir());
        placeholderMap.put(Placeholder.JAVA_HOME, javaHome);
        placeholderMap.put(Placeholder.JAR_PATH, String.format("%s/%s", getAbsoluteWorkDir(), jarFile.getName()));
        placeholderMap.put(Placeholder.JAR_NAME, jarFile.getName());
        placeholderMap.put(Placeholder.FILES, getFilesPlaceholderValue(getFilesToBeCompressed()));

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
        chmodX(scriptFiles);
        startup();
    }

    private void startup() throws Exception {
        log.debug("准备启动java应用({}) ...", jarFile.getName());
        executeCmd("./startup.sh");
        log.debug("已启动java应用!");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, JarGeneralCd> {
        private String javaHome;

        private Builder() {
        }

        public Builder javaHome(String javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        @Override
        public JarGeneralCd build() throws Exception {
            Assert.notNull(javaHome, "javaHome不能为空");
            log.debug("javaHome: {}", javaHome);

            // build
            return super.build().set(javaHome);
        }
    }

}

package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.net.util.PropertyPlaceholderHelper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * File持续部署
 *
 * @author xiangqian
 * @date 23:51 2022/07/26
 */
@Slf4j
public class FileCd extends AbstractCd {

    //
    private File[] srcFiles;

    // script
    private File[] scriptFiles;


    private void initScript() throws Exception {
        log.debug("准备初始化脚本 ...");
        // 校验脚本文件
        String[] scriptPaths = {"cd/file/clear.sh"};
        scriptFiles = checkScript(scriptPaths);

        // 定义以 "${" 开头，以 "}" 结尾的占位符
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put("ABSOLUTE_WORK_DIR", absoluteWorkDir);

        // 初始化脚本文件
        String content = null;
        for (File scriptFile : scriptFiles) {
            content = FileUtils.readFileToString(scriptFile, StandardCharsets.UTF_8);
            content = propertyPlaceholderHelper.replacePlaceholders(content, placeholderMap::get);
            FileUtils.write(scriptFile, content, StandardCharsets.UTF_8);
        }

        log.debug("已初始化脚本!");
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, FileCd> {
        private String[] srcFilePaths;

        private Builder() {
        }

        public Builder srcFilePaths(String... srcFilePaths) {
            this.srcFilePaths = srcFilePaths;
            return this;
        }

        @Override
        protected FileCd get() {
            return new FileCd();
        }

        @Override
        public FileCd build() throws Exception {

            // 校验资源文件路径
            File[] srcFiles = checkSrcFilePaths(srcFilePaths);

            //
            FileCd fileCd = super.build();
            fileCd.srcFiles = srcFiles;
            return fileCd;
        }
    }

}

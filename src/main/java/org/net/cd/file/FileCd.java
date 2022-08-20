package org.net.cd.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.net.cd.AbstractCd;
import org.net.cd.Placeholder;

import java.io.File;
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

    // source files
    private File[] sourceFiles;

    // script files
    private File[] scriptFiles;

    private FileCd() {
    }

    @Override
    protected void init() throws Exception {
        // 获取资源文件
        sourceFiles = getSource().get();

        // 获取脚本文件
        scriptFiles = getFilesOnClasspath("cd/file/clear.sh");
    }

    @Override
    protected void beforePost() throws Exception {
        log.debug("准备初始化脚本 ...");

        // 占位符参数
        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put(Placeholder.ABSOLUTE_WORK_DIR, getAbsoluteWorkDir());
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
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, FileCd> {
        private Builder() {
        }
    }

}

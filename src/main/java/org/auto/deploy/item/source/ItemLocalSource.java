package org.auto.deploy.item.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * 本地资源
 *
 * @author xiangqian
 * @date 23:07 2022/09/09
 */
@Slf4j
public class ItemLocalSource implements ItemSource {

    private Config config;
    private File file;
    private volatile File tempFile;

    public ItemLocalSource(Config config) {
        this.config = config;
        this.file = new File(config.getLocation());
        Assert.isTrue(file.exists(), String.format("本地资源位置不存在: %s", config.getLocation()));
        Assert.isTrue(file.isDirectory(), String.format("本地资源必须是目录类型: %s", config.getLocation()));
    }

    @Override
    public synchronized File get() throws Exception {
        if (!BooleanUtils.isTrue(config.getUseTempWorkspace())) {
            return file;
        }

        if (Objects.isNull(tempFile)) {
            log.debug("拷贝本地资源到临时目录下 ...\n\t{}", StringUtils.join(file.listFiles(), "\n\t"));

            // 获取临时目录，用于存放本地资源
            tempFile = Path.of(FileUtils.getTempDirectoryPath(), String.format("temp_%s", UUID.randomUUID().toString().replace("-", ""))).toFile();

            // 拷贝
            // FileUtils.copyDirectoryToDirectory(file, tempFile);
            for (File childFile : file.listFiles()) {
                FileUtils.copyToDirectory(childFile, tempFile);
            }
            log.debug("已拷贝本地资源到临时目录下!\n\t{}", tempFile.getAbsolutePath());
        }
        return tempFile;
    }

    @Override
    public void close() throws IOException {
        if (BooleanUtils.isTrue(config.getUseTempWorkspace())) {
            log.debug("删除本地资源临时目录! \n\t{}", tempFile.getAbsolutePath());
            FileUtils.forceDelete(tempFile);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Config {

        /**
         * 本地资源位置
         */
        private String location;

        /**
         * 是否使用临时工作空间
         * 如果设置为true，则会创建一个临时目录来处理资源；
         * 如果设置为false，则会在当前指定的位置处理资源；
         */
        @JsonProperty("use-temp-workspace")
        private Boolean useTempWorkspace;

        /**
         * cron定时执行表达式
         * 每30s执行任务:
         * 0/30 * * * * ?
         */
        private String cron;

        public void validate() {
            Assert.notNull(location, "source.local.location不能为null");
        }

    }

}

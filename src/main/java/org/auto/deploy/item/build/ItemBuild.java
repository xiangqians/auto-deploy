package org.auto.deploy.item.build;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.item.source.ItemSource;
import org.auto.deploy.util.CmdUtils;
import org.auto.deploy.util.OS;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 构建器
 *
 * @author xiangqian
 * @date 12:32 2022/09/10
 */
@Slf4j
public class ItemBuild implements Closeable {

    private Config config;
    private ItemSource source;

    public ItemBuild(Config config, ItemSource source) {
        this.config = config;
        this.source = source;
    }

    public void build() throws Exception {
        if (CollectionUtils.isEmpty(config.getCmds())) {
            return;
        }

        for (List<String> cmd : config.getCmds()) {
            List<String> newCmd = new ArrayList<>();
            OS os = OS.get();
            switch (os) {
                case WINDOWS:
                    newCmd.add(String.format("cd /d %s", source.get().getAbsolutePath()));
                    break;

                case LINUX:
                    newCmd.add(String.format("cd %s", source.get().getAbsolutePath()));
                    break;

                default:
                    throw new UnsupportedOperationException(String.format("目前暂不支持此操作系统: %s", os));
            }

            newCmd.add("&&");
            newCmd.addAll(cmd);
            String newCmdStr = StringUtils.join(newCmd, " ");
            log.debug("cmd: {}", newCmdStr);
            int exitValue = CmdUtils.execute(newCmdStr, Charset.forName("GBK"), System.out::println, System.err::println);
            if (exitValue != 0) {
                throw new Exception(String.format("exitValue: %s", exitValue));
            }
            log.debug("exitValue: {}", exitValue);
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Config {

        /**
         * 构建命令根路径
         */
        private String root;

        /**
         * 构建命令集
         */
        private List<List<String>> cmds;

        public void validate() {
        }

    }

}

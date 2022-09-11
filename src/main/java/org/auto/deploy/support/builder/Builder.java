package org.auto.deploy.support.builder;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.config.builder.BuilderConfig;
import org.auto.deploy.support.source.Source;
import org.auto.deploy.util.CmdUtils;
import org.auto.deploy.util.OS;

import java.util.ArrayList;
import java.util.List;

/**
 * 构建器
 *
 * @author xiangqian
 * @date 12:32 2022/09/10
 */
@Slf4j
public class Builder {

    private BuilderConfig config;
    private Source source;

    public Builder(BuilderConfig config, Source source) {
        this.config = config;
        this.source = source;
    }

    public void build() throws Exception {
        if (CollectionUtils.isEmpty(config.getCmds())) {
            return;
        }

        for (List<String> cmd : config.getCmds()) {
            List<String> newCmd = new ArrayList<>();
            newCmd.addAll(getCdCmd());
            newCmd.add("&&");
            newCmd.addAll(cmd);
            String newCmdStr = StringUtils.join(newCmd, " ");
            log.debug("{}", newCmdStr);
            CmdUtils.execute(newCmdStr, "GBK", System.out::println);
        }
    }

    protected List<String> getCdCmd() throws Exception {
        List<String> cd = new ArrayList<>(8);
        switch (OS.get()) {
            case WINDOWS:
                cd.add("cmd /c");
                cd.add(String.format("cd /d %s", source.get().getAbsolutePath()));
                break;

            case LINUX:
                cd.add(String.format("cd %s", source.get().getAbsolutePath()));
                break;
        }
        return cd;
    }

}

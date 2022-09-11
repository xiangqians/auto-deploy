package org.auto.deploy.utils;

import lombok.extern.slf4j.Slf4j;
import org.auto.deploy.util.CmdUtils;
import org.junit.Test;

import java.io.IOException;

/**
 * @author xiangqian
 * @date 12:58 2022/08/20
 */
@Slf4j
public class CmdUtilsTest {

    @Test
    public void test() throws IOException {
        String cmd = "java";
        CmdUtils.execute(cmd, "GBK", System.out::println);
    }


}

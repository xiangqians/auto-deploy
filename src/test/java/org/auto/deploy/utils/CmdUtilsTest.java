package org.auto.deploy.utils;

import lombok.extern.slf4j.Slf4j;
import org.auto.deploy.util.CmdUtils;
import org.junit.Test;

import java.nio.charset.Charset;

/**
 * @author xiangqian
 * @date 12:58 2022/08/20
 */
@Slf4j
public class CmdUtilsTest {

    @Test
    public void test() throws Exception {
        String[] cmdArray = new String[]{"java", "-version"};
        int exitValue = CmdUtils.execute(cmdArray, Charset.forName("GBK"), System.out::println, System.err::println);
        log.debug("exitValue: {}", exitValue);
    }

}

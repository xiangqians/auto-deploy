package org.net.util;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 命令行工具
 *
 * @author xiangqian
 * @date 13:09 2022/08/20
 */
public class CmdUtils {

    public static void execute(String cmd, String charsetName, Consumer<String> resultConsumer) throws IOException {
        Process process = null;
        try {
            process = execute(cmd);

            BufferedReader bufferedReader = null;

            // input
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), charsetName));
                String line = null;
                while (Objects.nonNull(line = bufferedReader.readLine())) {
                    resultConsumer.accept(line);
                }
            } finally {
                IOUtils.closeQuietly(bufferedReader);
            }

            // error
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), charsetName));
                String line = null;
                while (Objects.nonNull(line = bufferedReader.readLine())) {
                    resultConsumer.accept(line);
                }
            } finally {
                IOUtils.closeQuietly(bufferedReader);
            }

            // 等待子进程
//            process.waitFor();
        } finally {
            if (Objects.nonNull(process)) {
                process.destroy();
            }
        }
    }

    public static Process execute(String cmd) throws IOException {
        return Runtime.getRuntime().exec(cmd);
    }

}

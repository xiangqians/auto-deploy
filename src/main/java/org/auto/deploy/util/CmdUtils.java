package org.auto.deploy.util;

import lombok.SneakyThrows;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 命令行工具
 *
 * @author xiangqian
 * @date 13:09 2022/08/20
 */
public class CmdUtils {

    public static int execute(String cmd, Charset charset, Consumer<String> input, Consumer<String> error) throws IOException, InterruptedException {
        return execute(new String[]{cmd}, charset, input, error);
    }

    /**
     * execute
     *
     * @param cmdArray cmdArray参数只是一个可运行的命令或者脚本，并不等效于Shell解器或者Cmd.exe
     * @param charset
     * @param input
     * @param error
     * @return exitValue
     * @throws IOException
     * @throws InterruptedException
     */
    public static int execute(String[] cmdArray, Charset charset, Consumer<String> input, Consumer<String> error) throws IOException, InterruptedException {
        Process process = null;
        try {
            // Process
            // 不同平台上，命令的兼容性
            OS os = OS.get();
            switch (os) {
                case WINDOWS:
                    // Windows NT: ?
                    // Windows 95: ?
                    // Windows 10: cmd /c
                    cmdArray = ListUtils.union(List.of("cmd", "/C"), Arrays.asList(cmdArray)).toArray(String[]::new);
                    break;

                case LINUX:
                    cmdArray = ListUtils.union(List.of("/bin/sh", "-c"), Arrays.asList(cmdArray)).toArray(String[]::new);
                    break;

                default:
                    throw new UnsupportedOperationException(String.format("目前暂不支持此操作系统: %s", os));
            }
            process = Runtime.getRuntime().exec(cmdArray);

            // input
            StreamReader.read(process.getInputStream(), charset, input);

            // error
            StreamReader.read(process.getErrorStream(), charset, error);

            // 等待外部进程处理完成，并获取外部进程的返回值
            int exitValue = process.waitFor();
            return exitValue;
        } finally {
            if (Objects.nonNull(process)) {
                process.destroy();
            }
        }
    }

    public static class StreamReader implements Runnable {

        private InputStream inputStream;
        private Charset charset;
        private Consumer<String> consumer;

        public StreamReader(InputStream inputStream, Charset charset, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.charset = charset;
            this.consumer = consumer;
        }

        @SneakyThrows
        @Override
        public void run() {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset));
                String line = null;
                while (Objects.nonNull(line = bufferedReader.readLine())) {
                    consumer.accept(line);
                }
            } finally {
                IOUtils.closeQuietly(bufferedReader);
            }
        }

        public static void read(InputStream inputStream, Charset charset, Consumer<String> consumer) {
            new Thread(new StreamReader(inputStream, charset, consumer)).start();
        }
    }

}

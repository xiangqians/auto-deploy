package org.net.cd;

import com.jcraft.jsch.JSchException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.net.cd.source.Source;
import org.net.sftp.FileTransferMode;
import org.net.sftp.Sftp;
import org.net.sftp.impl.DefaultSftpProgressMonitor;
import org.net.sftp.impl.JSchSftpImpl;
import org.net.ssh.ConnectionProperties;
import org.net.ssh.Ssh;
import org.net.ssh.impl.JSchShellChannelSshImpl;
import org.net.util.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author xiangqian
 * @date 22:34 2022/07/25
 */
@Slf4j
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractCd implements Cd {

    // ssh & sftp
    private ConnectionProperties connectionProperties;
    private Duration sessionConnectTimeout;
    private Duration channelConnectTimeout;
    private Ssh ssh;
    private Sftp sftp;

    // 工作路径
    private String workDir;
    // 绝对工作路径
    private String absoluteWorkDir;

    // sudo
    private boolean sudo;

    // source
    private Source source;

    // .tar.gz
    private File tarGzFile;

    // 错误集
    private Set<String> errorSet = Set.of("No such file or directory",
            "Permission denied"
    );

    protected final void set(ConnectionProperties connectionProperties,
                             Duration sessionConnectTimeout,
                             Duration channelConnectTimeout,
                             String workDir,
                             boolean sudo,
                             Source source) {
        this.connectionProperties = connectionProperties;
        this.sessionConnectTimeout = sessionConnectTimeout;
        this.channelConnectTimeout = channelConnectTimeout;
        this.workDir = workDir;
        this.sudo = sudo;
        this.source = source;
    }

    protected void init() throws Exception {
    }

    private void connect() throws JSchException {
        log.debug("准备连接 {} 服务器 ...", connectionProperties.getHost());
        ssh = JSchShellChannelSshImpl.builder()
                .connectionProperties(connectionProperties)
                .sessionConnectTimeout(sessionConnectTimeout)
                .channelConnectTimeout(channelConnectTimeout)
                .defaultQuickEndSignPatterns()
                .build();
        sftp = JSchSftpImpl.builder()
                .connectionProperties(connectionProperties)
                .sessionConnectTimeout(sessionConnectTimeout)
                .channelConnectTimeout(channelConnectTimeout)
                .build();
        log.debug("已成功连接到服务器!");
    }

    private void cdWorkDir() throws Exception {
        log.debug("准备进入工作目录({}) ...", workDir);
        String cmd = null;

        // ssh
        cmd = String.format("cd %s", workDir);
        executeCmd(cmd, true);

        // pwd
        cmd = "pwd";
        List<String> results = ssh.execute(cmd);
        log.debug("<ssh> {}\n{}", cmd, StringUtils.join(results, "\n"));
        if (CollectionUtils.isNotEmpty(results)) {
            for (String result : results) {
                if (result.startsWith("/")) {
                    absoluteWorkDir = result;
                    break;
                }
            }
        }
        Assert.notNull(absoluteWorkDir, "无法解析绝对工作路径");
        log.debug("absoluteWorkDir: {}", absoluteWorkDir);

        // sftp
        sftp.cd(workDir);
        log.debug("<sftp> cd {}", workDir);

        log.debug("已进入工作目录!");
    }

    private void clear() throws Exception {
        log.debug("准备清除上一个版本信息 ...");
        executeCmd("./clear.sh");
        log.debug("已清除上一个版本信息!");
    }

    /**
     * 压缩要上传的文件或文件夹
     */
    private void compress() throws IOException {
        log.debug("准备压缩本地文件或文件夹 ...");

        // 获取临时目录，用于压缩后的文件（tar.gz）
        String tempDirPath = FileUtils.getTempDirectoryPath();
        log.debug("tempDirPath: {}", tempDirPath);

        // 压缩
        String tarGzFileName = String.format("temp_%s.tar.gz", UUID.randomUUID().toString().replace("-", ""));
        tarGzFile = new File(tempDirPath + File.separator + tarGzFileName);
        CompressionUtils.tarGz(getFilesToBeCompressed(), tarGzFile);
        log.debug("已压缩本地文件或文件夹! (压缩文件为 {})", tarGzFile.getAbsolutePath());
    }

    /**
     * 获取将要压缩的文件或文件夹集
     *
     * @return
     */
    protected abstract File[] getFilesToBeCompressed();

    /**
     * 上传压缩包
     *
     * @throws Exception
     */
    private void uploadArchive() throws Exception {
        log.debug("准备上传压缩文件({}) ...", tarGzFile.getAbsolutePath());
        sftp.put(tarGzFile.getAbsolutePath(), tarGzFile.getName(), DefaultSftpProgressMonitor.builder().build(), FileTransferMode.OVERWRITE);
        log.debug("已上传压缩文件!");
    }

    /**
     * 解压
     *
     * @throws Exception
     */
    private void decompress() throws Exception {
        log.debug("准备解压文件({}) ...", tarGzFile.getName());
        String cmd = String.format("tar -zxvf ./%s", tarGzFile.getName());
        executeCmd(cmd, Duration.ofMinutes(5));
        log.debug("已解压文件!");
    }

    /**
     * 删除压缩包
     *
     * @throws Exception
     */
    private void deleteArchive() throws Exception {
        log.debug("准备删除压缩文件({}) ...", tarGzFile.getName());
        executeCmd(String.format("rm -rf ./%s", tarGzFile.getName()));
        log.debug("已删除压缩文件!");
    }

    /**
     * 前置处理器，在进入工作目录之后调此方法
     *
     * @throws Exception
     */
    protected void beforePost() throws Exception {
    }

    /**
     * 后置处理器，在把相关文件都上传到服务器后调此方法
     *
     * @throws Exception
     */
    protected void afterPost() throws Exception {
    }

    @Override
    public void execute() throws Exception {
        try {
            init();
            connect();
            cdWorkDir();
            beforePost();
            clear();
            compress();
            uploadArchive();
            decompress();
            afterPost();
            deleteArchive();
        } finally {
            if (Objects.nonNull(tarGzFile)) {
                FileUtils.forceDelete(tarGzFile);
            }
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(source, ssh, sftp);
    }

    protected final void executeCmd(String cmd) throws Exception {
        executeCmd(cmd, false);
    }

    protected final void executeCmd(String cmd, boolean isVerifyResult) throws Exception {
        executeCmd(cmd, Ssh.DEFAULT_TIMEOUT, isVerifyResult);
    }

    protected final void executeCmd(String cmd, Duration timeout) throws Exception {
        executeCmd(cmd, timeout, false);
    }

    /**
     * 执行命令
     *
     * @param cmd            命令
     * @param timeout        执行命令超时时间
     * @param isVerifyResult 校验命令执行结果是否异常
     * @throws Exception
     */
    protected final void executeCmd(String cmd, Duration timeout, boolean isVerifyResult) throws Exception {
        if (!isVerifyResult) {
            ssh.execute(cmd, timeout, new Consumer<>() {
                private int index = 0;

                @Override
                public void accept(String result) {
                    if (index == 0) {
                        log.debug("{}", cmd);
                    }
                    index++;
//                System.out.format("[%03d] %s", index++, result).println();
                    System.out.format("%s", result).println();
                }
            });
            return;
        }

        List<String> results = ssh.execute(cmd, timeout);
        log.debug("<ssh> {}\n{}", cmd, StringUtils.join(results, "\n"));
        if (isVerifyResult) {
            for (String error : errorSet) {
                Assert.isTrue(ListUtils.indexOf(results, result -> result.contains(error)) == -1, String.format("%s: %s", cmd, error));
            }
        }
    }

    /**
     * 授予文件拥有可执行权限
     *
     * @param files
     * @throws Exception
     */
    protected final void chmodX(File... files) throws Exception {
        log.debug("准备授予文件拥有可执行权限 ...");
        if (ArrayUtils.isNotEmpty(files)) {
            for (File file : files) {
                if (file.isDirectory()) {
                    continue;
                }
                String fileName = file.getName();
                log.debug("授予 {} 可执行权限!", fileName);
                ssh.execute(String.format("chmod +x ./%s", fileName));
            }
        }
        log.debug("已授予文件拥有可执行权限!");
    }

    /**
     * 获取类路径下的文件集
     *
     * @param filePaths
     * @return
     */
    public static File[] getFilesOnClasspath(String... filePaths) {
        int length = filePaths.length;
        File[] files = new File[length];
        for (int i = 0; i < length; i++) {
            String filePath = filePaths[i];
            URL url = Thread.currentThread().getContextClassLoader().getResource(filePath);
            Assert.notNull(url, String.format("未找到 %s 文件或者文件夹", filePath));
            File file = new File(url.getFile());
            Assert.isTrue(file.exists(), String.format("%s 文件或者文件夹不存在", filePath));
            files[i] = file;
        }

        return files;
    }

    public static File[] getFiles(String... filePaths) {
        int length = filePaths.length;
        File[] files = new File[length];
        for (int i = 0; i < length; i++) {
            String filePath = filePaths[i];
            Assert.notNull(filePath, String.format("filePaths[%s]为空!", i));
            File file = new File(filePath);
            Assert.isTrue(file.exists(), String.format("%s 文件或者文件夹不存在", filePath));
            files[i] = file;
        }
        return files;
    }

    public static File getJarFile(File... files) throws FileNotFoundException {
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.endsWith(".jar")) {
                return file;
            }
        }
        throw new FileNotFoundException("无法获取jar文件!");
    }

    /**
     * 获取FILES占位符值
     *
     * @param files
     * @return
     * @throws IOException
     */
    public static String getFilesPlaceholderValue(File... files) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>(files.length);
        for (File file : files) {
            list.add(Map.of("name", file.getName(), "isDir", file.isDirectory()));
        }
        return JacksonUtils.toJson(list);
    }

    /**
     * 替换文件占位符
     *
     * @param files
     * @param placeholderMap
     * @throws Exception
     */
    public static void replacePlaceholders(File[] files, Map<String, String> placeholderMap) throws Exception {
        // 定义以 "${" 开头，以 "}" 结尾的占位符
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");

        // 初始化脚本文件
        StringBuilder content = new StringBuilder();
        for (File file : files) {
            content.setLength(0);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(file));
                String line = null;
                while (Objects.nonNull(line = br.readLine())) {
                    line = propertyPlaceholderHelper.replacePlaceholders(line, placeholderMap::get);
                    if (line.startsWith("JS \"")) {
                        Function<String, String> preFunction = script -> {
                            if (script.endsWith("\\") || script.endsWith("\"")) {
                                script = script.substring(0, script.length() - 1);
                            }
                            script = script.replace(" out(", "result.push(");
                            script = propertyPlaceholderHelper.replacePlaceholders(script, placeholderMap::get);
                            return script;
                        };
                        StringBuilder jsScriptBuilder = new StringBuilder();
                        jsScriptBuilder.append("function execute(){").append('\n');
                        jsScriptBuilder.append('\t').append("var result = [];").append('\n');
                        jsScriptBuilder.append('\t').append(preFunction.apply(line.substring("JS \"".length()))).append('\n');
                        if (!line.endsWith("\"")) {
                            while (Objects.nonNull(line = br.readLine())) {
                                jsScriptBuilder.append(preFunction.apply(line)).append('\n');
                                if (line.endsWith("\"")) {
                                    break;
                                }
                            }
                        }
                        jsScriptBuilder.append('\t').append("return result;").append('\n');
                        jsScriptBuilder.append("};").append('\n');
                        jsScriptBuilder.append("execute();");
                        List<Object> result = JavaScriptUtils.execute(jsScriptBuilder.toString(), null, List.class);
                        content.append(StringUtils.join(result, '\n')).append('\n');
                        continue;
                    }
                    content.append(line).append('\n');
                }
            } finally {
                IOUtils.closeQuietly(br);
            }
            FileUtils.write(file, content, StandardCharsets.UTF_8);
        }
    }

    protected static abstract class Builder<B extends Builder, A extends AbstractCd> {

        private ConnectionProperties connectionProperties;
        private Duration sessionConnectTimeout;
        private Duration channelConnectTimeout;
        private String workDir;
        private boolean sudo;
        private Source source;
        private Class<A> type;

        protected Builder() {
            sudo = true;

            // 获取泛型类型
            Type genType = this.getClass().getGenericSuperclass();
            if (genType instanceof ParameterizedType) {
                Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
                if (ArrayUtils.isNotEmpty(params) && params.length > 1) {
                    type = (Class<A>) params[1];
                    log.debug("type: {}", type);
                }
            }
        }

        public B connectionProperties(ConnectionProperties connectionProperties) {
            this.connectionProperties = connectionProperties;
            return (B) this;
        }

        public B sessionConnectTimeout(Duration channelConnectTimeout) {
            this.sessionConnectTimeout = channelConnectTimeout;
            return (B) this;
        }

        public B channelConnectTimeout(Duration channelConnectTimeout) {
            this.channelConnectTimeout = channelConnectTimeout;
            return (B) this;
        }

        public B workDir(String workDir) {
            this.workDir = workDir;
            return (B) this;
        }

        public B sudo(boolean sudo) {
            this.sudo = sudo;
            return (B) this;
        }

        public B source(Source source) {
            this.source = source;
            return (B) this;
        }

        public A build() throws Exception {
            // workDir
            Assert.notNull(workDir, "workDir不能为空");
            log.debug("workDir: {}", workDir);

            // source
            Assert.notNull(source, "source不能为空");
            log.debug("source: {}", source);

            // new Constructor
            Constructor<A> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            A a = constructor.newInstance();

            // set
            a.set(connectionProperties, sessionConnectTimeout, channelConnectTimeout, workDir, sudo, source);
            return a;
        }

    }

}

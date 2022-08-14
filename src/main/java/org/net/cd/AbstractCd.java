package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.net.sftp.FileTransferMode;
import org.net.sftp.Sftp;
import org.net.sftp.impl.DefaultSftpProgressMonitor;
import org.net.sftp.impl.JSchSftpImpl;
import org.net.ssh.ConnectionProperties;
import org.net.ssh.Ssh;
import org.net.ssh.impl.JSchShellChannelSshImpl;
import org.net.util.*;

import java.io.*;
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
public abstract class AbstractCd implements Cd {

    protected ConnectionProperties connectionProperties;
    protected Duration sessionConnectTimeout;
    protected Duration channelConnectTimeout;
    protected Ssh ssh;
    protected Sftp sftp;
    // 工作路径
    protected String workDir;
    // 绝对工作路径
    protected String absoluteWorkDir;

    // .tar.gz
    private File tarGzFile;

    protected void init(ConnectionProperties connectionProperties,
                        Duration sessionConnectTimeout,
                        Duration channelConnectTimeout,
                        String workDir) throws Exception {
        this.connectionProperties = connectionProperties;
        this.sessionConnectTimeout = sessionConnectTimeout;
        this.channelConnectTimeout = channelConnectTimeout;
        this.workDir = workDir;
        this.ssh = JSchShellChannelSshImpl.builder()
                .connectionProperties(connectionProperties)
                .sessionConnectTimeout(sessionConnectTimeout)
                .channelConnectTimeout(channelConnectTimeout)
                .defaultQuickEndSignPatterns()
                .build();
        this.sftp = JSchSftpImpl.builder()
                .connectionProperties(connectionProperties)
                .sessionConnectTimeout(sessionConnectTimeout)
                .channelConnectTimeout(channelConnectTimeout)
                .build();
    }

    private void cdWorkDir() throws Exception {
        String cmd = null;
        List<String> results = null;

        // ssh
        cmd = String.format("cd %s", workDir);
        results = ssh.execute(cmd);
        log.debug("<ssh> {}\n{}", cmd, StringUtils.join(results, "\n"));
        Assert.isTrue(ListUtils.indexOf(results, result -> result.contains("No such file or directory")) == -1,
                String.format("%s: No such file or directory", workDir));

        // pwd
        cmd = "pwd";
        results = ssh.execute(cmd);
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

        // sftp
        sftp.cd(workDir);
        log.debug("<sftp> cd {}", workDir);
    }

    protected String getFilesPlaceholderValue() throws IOException {
        return getFilesPlaceholderValue(getFilesToBeCompressed());
    }

    protected String getFilesPlaceholderValue(File... files) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>(files.length);
        for (File srcFile : files) {
            list.add(Map.of("name", srcFile.getName(), "isDir", srcFile.isDirectory()));
        }
        return JacksonUtils.toJson(list);
    }

    protected void replacePlaceholders(File[] scriptFiles, Map<String, String> placeholderMap) throws Exception {
        // 定义以 "${" 开头，以 "}" 结尾的占位符
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");

        // 初始化脚本文件
        StringBuilder content = new StringBuilder();
        for (File scriptFile : scriptFiles) {
            content.setLength(0);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(scriptFile));
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
            FileUtils.write(scriptFile, content, StandardCharsets.UTF_8);
        }
    }

    private void clear() throws Exception {
        log.debug("准备清除上一个版本信息 ...");
        String cmd = "./clear.sh";
        ssh.execute(cmd, resultConsumer(cmd));
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
        ssh.execute(cmd, Duration.ofMinutes(5), resultConsumer(cmd));
        log.debug("已解压文件!");
    }

    /**
     * 修改文件或文件夹权限
     *
     * @throws Exception
     */
    private void chmodScriptFiles() throws Exception {
        log.debug("准备修改文件或文件夹权限 ...");
        File[] scriptFiles = getScriptFiles();
        if (ArrayUtils.isNotEmpty(scriptFiles)) {
            for (File scriptFile : scriptFiles) {
                String fileName = scriptFile.getName();
                log.debug("授予 {} 可执行权限!", fileName);
                ssh.execute(String.format("chmod +x %s", fileName));
            }
        }
        log.debug("已修改文件或文件夹权限!");
    }

    /**
     * 获取脚本文件集
     *
     * @return
     */
    protected abstract File[] getScriptFiles();

    /**
     * 删除压缩包
     *
     * @throws Exception
     */
    private void deleteArchive() throws Exception {
        log.debug("准备删除压缩文件({}) ...", tarGzFile.getName());
        String cmd = String.format("rm -rf ./%s", tarGzFile.getName());
        ssh.execute(cmd, resultConsumer(cmd));
        log.debug("已删除压缩文件!");
    }

    /**
     * 压缩之前处理
     *
     * @throws Exception
     */
    protected void compressBeforePost() throws Exception {
    }

    /**
     * 压缩之后处理
     *
     * @throws Exception
     */
    protected void decompressAfterPost() throws Exception {
    }

    @Override
    public final void execute() throws Exception {
        try {
            cdWorkDir();
            compressBeforePost();
            clear();
            compress();
            uploadArchive();
            decompress();
            chmodScriptFiles();
            decompressAfterPost();
            deleteArchive();
        } finally {
            if (Objects.nonNull(tarGzFile)) {
                FileUtils.forceDelete(tarGzFile);
            }
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(ssh, sftp);
    }

    protected static File getJarFile(File... srcFiles) throws FileNotFoundException {
        for (File srcFile : srcFiles) {
            String fileName = srcFile.getName();
            if (fileName.endsWith(".jar")) {
                return srcFile;
            }
        }
        throw new FileNotFoundException("无法获取jar文件!");
    }

    protected static File[] checkSrcFilePaths(String... srcFilePaths) {
        Assert.notNull(srcFilePaths, "srcFilePaths不能为空!");
        int length = srcFilePaths.length;
        File[] srcFiles = new File[length];
        for (int i = 0; i < length; i++) {
            String filePath = srcFilePaths[i];
            Assert.notNull(filePath, String.format("srcFilePaths[%s]不能为空!", i));
            File file = new File(filePath);
            Assert.isTrue(file.exists(), String.format("srcFilePaths[%s]: %s 文件或者文件夹不存在!", i, file));
            srcFiles[i] = file;
        }
        return srcFiles;
    }

    /**
     * 校验脚本文件
     */
    protected File[] checkScript(String[] scriptPaths) {
        int length = scriptPaths.length;
        File[] scriptFiles = new File[length];
        for (int i = 0; i < length; i++) {
            String scriptPath = scriptPaths[i];
            URL scriptUrl = this.getClass().getClassLoader().getResource(scriptPath);
            Assert.notNull(scriptUrl, String.format("未找到 %s 文件", scriptPath));
            File scriptFile = new File(scriptUrl.getFile());
            Assert.isTrue(scriptFile.exists(), String.format("%s 文件不存在", scriptPath));
            scriptFiles[i] = scriptFile;
        }
        return scriptFiles;
    }

    public static Consumer<String> resultConsumer(String cmd) {
        return new Consumer<>() {
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
        };
    }

    protected static abstract class Builder<B extends Builder, A extends AbstractCd> {

        protected ConnectionProperties connectionProperties;
        protected Duration sessionConnectTimeout;
        protected Duration channelConnectTimeout;
        protected String workDir;

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

        protected abstract A get();

        public A build() throws Exception {
            Assert.notNull(workDir, "workDir不能为空");
            A a = get();
            a.init(connectionProperties, sessionConnectTimeout, channelConnectTimeout, workDir);
            return a;
        }

    }

}

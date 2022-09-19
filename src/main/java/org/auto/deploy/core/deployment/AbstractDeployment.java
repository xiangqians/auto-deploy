package org.auto.deploy.core.deployment;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.core.server.Server;
import org.auto.deploy.util.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

/**
 * @author xiangqian
 * @date 16:32 2022/09/10
 */
@Slf4j
public abstract class AbstractDeployment implements Deployment {

    protected Server server;

    // .tar.gz
    private File tempTarGzFile;

    // temp dir
    private File tempDir;

    public AbstractDeployment(Server server) {
        this.server = server;
    }

    @Override
    public final void deploy() throws Exception {
        try {
            createTempDir();
            init();
            clean();
            compress();
            uploadArchive();
            decompress();
            deleteArchive();
            afterPost();
        } finally {
            close();
        }
    }

    protected void afterPost() throws Exception {
    }

    private void deleteArchive() throws Exception {
        log.debug("删除服务器上的压缩文件 ...\n\t{}", tempTarGzFile.getName());
        server.executeCmd(String.format("rm -rf ./%s", tempTarGzFile.getName()));
        log.debug("已删除服务器上的压缩文件!\n\t{}", tempTarGzFile.getName());
    }

    private void decompress() throws Exception {
        log.debug("解压服务器上的压缩文件 ...\n\t{}", tempTarGzFile.getName());
        String cmd = String.format("tar -zxvf ./%s", tempTarGzFile.getName());
        server.executeCmd(cmd, Duration.ofMinutes(5));
        log.debug("已解压服务器上的压缩文件!\n\t{}", tempTarGzFile.getName());
    }

    private void uploadArchive() throws Exception {
        log.debug("上传压缩文件到服务器 ...\n\t{}", tempTarGzFile.getAbsolutePath());
        server.uploadFile(tempTarGzFile.getAbsolutePath(), tempTarGzFile.getName());
        log.debug("已上传压缩文件到服务器!\n\t{}", tempTarGzFile.getAbsolutePath());
    }

    private void compress() throws IOException {
        File[] files = getFiles();
        log.debug("压缩资源文件 ...\n\t{}", StringUtils.join(files, "\n\t"));

        // 获取临时目录，用于压缩后的文件（tar.gz）
        tempTarGzFile = Path.of(FileUtils.getTempDirectoryPath(), String.format("temp_%s.tar.gz", UUID.randomUUID().toString().replace("-", ""))).toFile();

        // 压缩
        CompressionUtils.tarGz(files, tempTarGzFile);
        log.debug("已压缩压资源文件!\n\t{}", tempTarGzFile.getAbsolutePath());
    }

    /**
     * 获取预部署的文件集
     *
     * @return
     */
    protected abstract File[] getFiles();

    private void clean() throws Exception {
        log.debug("清除上一个版本信息 ...");
        server.executeCmd("./clean.sh");
        log.debug("已清除上一个版本信息!");
    }

    protected abstract void init() throws Exception;

    private void createTempDir() {
        tempDir = Path.of(FileUtils.getTempDirectoryPath(), String.format("temp_%s", UUID.randomUUID().toString().replace("-", ""))).toFile();
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        log.debug("创建临时目录!\n\t", tempDir.getAbsolutePath());
    }

    // ====================

    /**
     * 替换脚本资源占位符
     *
     * @param url
     * @param placeholderMap
     * @return
     * @throws Exception
     */
    protected File replaceScriptResourcePlaceholders(URL url, Map<String, Object> placeholderMap) throws Exception {

        // Object -> String
        Map<String, String> placeholderStrMap = new HashMap<>(placeholderMap.size());
        for (Map.Entry<String, Object> entry : placeholderMap.entrySet()) {
            Object value = entry.getValue();
            String strValue = null;
            if (Objects.isNull(value)) {
                // null

            } else if (value instanceof Boolean
                    || value instanceof Integer
                    || value instanceof Long
                    || value instanceof String) {
                strValue = value.toString();

            } else if (value instanceof FilesPlaceholderValue) {
                strValue = ((FilesPlaceholderValue) value).serialize();

            } else {
                throw new IllegalArgumentException(String.format("目前暂不支持此类型解析: %s", value.getClass()));
            }
            placeholderStrMap.put(entry.getKey(), strValue);
        }

        // 定义以 "${" 开头，以 "}" 结尾的占位符
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");

        // 初始化脚本文件
        StringBuilder content = new StringBuilder();
        BufferedReader br = null;
        try {
            // org.apache.commons.io.IOUtils.toString(java.net.URL, java.nio.charset.Charset)
            if ("file".equals(url.getProtocol())) {
                br = new BufferedReader(new FileReader(url.getFile()));
            } else if ("jar".equals(url.getProtocol())) {
                br = new BufferedReader(new InputStreamReader(url.openStream()));
            } else {
                throw new UnknownError(String.format("未知URL协议: %s", url.getProtocol()));
            }

            String line = null;
            while (Objects.nonNull(line = br.readLine())) {
                line = propertyPlaceholderHelper.replacePlaceholders(line, placeholderStrMap::get);
                if (line.startsWith("JS \"")) {
                    Function<String, String> preFunction = script -> {
                        if (script.endsWith("\\") || script.endsWith("\"")) {
                            script = script.substring(0, script.length() - 1);
                        }
                        script = script.replace(" out(", "result.push(");
                        script = propertyPlaceholderHelper.replacePlaceholders(script, placeholderStrMap::get);
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

        log.debug("替换文件占位符: {}\n{}", url.getFile(), content);

        File newFile = Path.of(tempDir.getAbsolutePath(), new File(url.getFile()).getName()).toFile();
        FileUtils.write(newFile, content, StandardCharsets.UTF_8);
        return newFile;
    }

    /**
     * 获取附加文件
     *
     * @param basePath
     * @param name
     * @return
     */
    protected File getAddlFile(String basePath, String name) {
        File addlFile = null;
        // 绝对路径
        if (name.contains(":") || name.startsWith("/")) {
            addlFile = new File(name);
        }
        // 相对路径
        // 将相对路径修改为绝对路径
        else {
            addlFile = Path.of(basePath, name).toFile();
        }

        Assert.isTrue(Objects.nonNull(addlFile) && addlFile.exists(),
                String.format("附加文件不存在: %s", Optional.ofNullable(addlFile).map(File::getAbsolutePath).orElse(name)));
        return addlFile;
    }

    /**
     * 获取脚本资源
     *
     * @param name
     * @return
     */
    protected URL getScriptResource(String name) {
        name = String.format("config/%s", name);
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        Assert.notNull(url, String.format("未找到 %s 脚本文件!", name));
        return url;
    }

    protected void chmodX(File... files) throws Exception {
        if (ArrayUtils.isNotEmpty(files)) {
            chmodX(Arrays.stream(files)
                    .map(File::getName)
                    .map(fileName -> String.format("./%s", fileName))
                    .toArray(String[]::new));
        }
    }

    /**
     * 授予文件可执行权限
     *
     * @param files
     * @throws Exception
     */
    protected void chmodX(String... files) throws Exception {
        if (ArrayUtils.isNotEmpty(files)) {
            log.debug("授予文件拥有可执行权限 ...\n\t{}", StringUtils.join(files, ", "));
            server.executeCmd(String.format("chmod +x %s", StringUtils.join(files, " ")));
            log.debug("已授予文件拥有可执行权限!\n\t{}", StringUtils.join(files, ", "));
        }
    }

    public static class FilesPlaceholderValue {

        List<Map<String, Object>> value;

        public FilesPlaceholderValue() {
            value = new ArrayList<>();
        }

        public FilesPlaceholderValue add(URL url) {
            add(String.format("./%s", new File(url.getFile()).getName()), false);
            return this;
        }

        public FilesPlaceholderValue add(File file) {
            if (Objects.nonNull(file) && file.exists()) {
                add(String.format("./%s", file.getName()), file.isDirectory());
            }
            return this;
        }

        public FilesPlaceholderValue add(String name, boolean isDir) {
            value.add(Map.of("name", name, "isDir", isDir));
            return this;
        }

        public String serialize() {
            try {
                return JacksonUtils.toJson(value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(tempTarGzFile)) {
            try {
                FileUtils.forceDelete(tempTarGzFile);
            } catch (Exception e) {
                log.error("", e);
            } finally {
                tempTarGzFile = null;
            }
        }
        if (Objects.nonNull(tempDir)) {
            try {
                FileUtils.forceDelete(tempDir);
            } catch (Exception e) {
                log.error("", e);
            } finally {
                tempDir = null;
            }
        }
    }

    public static void closeQuietly(AutoCloseable... autoCloseables) {
        if (ArrayUtils.isEmpty(autoCloseables)) {
            return;
        }

        for (AutoCloseable autoCloseable : autoCloseables) {
            if (Objects.nonNull(autoCloseable)) {
                try {
                    autoCloseable.close();
                } catch (Exception e) {
                }
            }
        }
    }

}

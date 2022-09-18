package org.auto.deploy.item;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.util.JacksonUtils;
import org.springframework.util.Assert;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author xiangqian
 * @date 21:26 2022/09/14
 */
@Slf4j
public class ItemService {

    public static final String ITEMS_PATHNAME;
    private static final File ITEMS_DIR;
    private static final Pattern PATTERN;
    private static final Map<String, ItemDeployer> ITEM_DEPLOYER_MAP;

    static {
        // items pathname
        ITEMS_PATHNAME = "items";

        // items dir
        ITEMS_DIR = new File(ITEMS_PATHNAME);
        try {
            FileUtils.forceMkdir(ITEMS_DIR);
            log.debug("itemsDir: {}", ITEMS_DIR.getAbsolutePath());
        } catch (IOException e) {
            throw new Error(e);
        }

        // pattern
        PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");

        // item deployer map
        ITEM_DEPLOYER_MAP = new ConcurrentHashMap<>();
    }

    public static Boolean deploy(String itemName) throws Exception {
        ItemDeployer itemDeployer = ITEM_DEPLOYER_MAP.get(itemName);
        if (Objects.nonNull(itemDeployer) && itemDeployer.isAlive()) {
            throw new RuntimeException("当前项目正在部署中，请稍后再试!");
        }

        itemDeployer = new ItemDeployer(itemName);
        itemDeployer.start();
        return true;
    }

    public static ItemConfig getItemConfig(String itemName) throws IOException {
        InputStream input = null;
        try {
            Yaml yaml = new Yaml();
            return JacksonUtils.toObject(yaml.loadAs(getFileContent(itemName, "config", "core.yml"), Map.class), ItemConfig.class);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    public static Boolean modifyFileContent(String itemName, String[] fileNames, String content) throws IOException {
        File file = getFile(itemName, fileNames);
        Assert.isTrue(file.exists(), String.format("FileNotFoundException: %s", file.getAbsolutePath()));
        FileUtils.write(file, content, StandardCharsets.UTF_8);
        return true;
    }

    public static String getFileContent(String itemName, String... fileNames) throws IOException {
        File file = getFile(itemName, fileNames);
        Assert.isTrue(file.exists(), String.format("FileNotFoundException: %s", file.getAbsolutePath()));
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    public static List<String> getLogList(String itemName) {
        File logDir = getLogDir(itemName);
        if (!logDir.exists()) {
            return Collections.emptyList();
        }

        return Optional.ofNullable(logDir.listFiles(File::isFile))
                .filter(ArrayUtils::isNotEmpty)
                .map(files -> Arrays.stream(files).map(File::getName).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static synchronized Boolean add(String itemName) throws IOException {
        itemName = StringUtils.trimToEmpty(itemName);
        Assert.isTrue(PATTERN.matcher(itemName).matches(), "预部署项目名称必须以字母开头，并且只能输入字母、数字、下划线、横杠，长度不能超过32个字符!");

        String finalItemName = itemName;
        if (Arrays.stream(ITEMS_DIR.listFiles(File::isDirectory))
                .filter(file -> file.getName().equals(finalItemName))
                .count() > 0) {
            throw new IllegalArgumentException("项目名已存在!");
        }

        // config
        String configPath = String.format("%s/%s/config", ITEMS_PATHNAME, itemName);
        FileUtils.forceMkdir(new File(configPath));

        // config/core.yml
        String coreYmlContent = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResource("config/core.yml"), StandardCharsets.UTF_8);
        FileUtils.write(new File(String.format("%s/core.yml", configPath)), coreYmlContent, StandardCharsets.UTF_8);

        // config/jar/docker/Dockerfile
        String dockerPath = String.format("%s/jar/docker", configPath);
        FileUtils.forceMkdir(new File(dockerPath));
        String dockerfileContent = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResource("config/jar/docker/Dockerfile"), StandardCharsets.UTF_8);
        FileUtils.write(new File(String.format("%s/Dockerfile", dockerPath)), dockerfileContent, StandardCharsets.UTF_8);

        // logs
        FileUtils.forceMkdir(getLogDir(itemName));

        // info.json
        ItemInfo itemInfo = new ItemInfo();
        itemInfo.setName(itemName);
        FileUtils.write(getInfoFile(itemName), JacksonUtils.toJson(itemInfo), StandardCharsets.UTF_8);

        return true;
    }

    public static List<ItemInfo> getItemInfos() {
        return Optional.ofNullable(ITEMS_DIR.listFiles(File::isDirectory))
                .filter(ArrayUtils::isNotEmpty)
                .map(files -> Arrays.stream(files).map(file -> getItemInfo(file.getName())).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static ItemInfo getItemInfo(String itemName) {
        try {
            File infoFile = getInfoFile(itemName);
            Assert.isTrue(infoFile.exists(), String.format("FileNotFoundException: %s", infoFile.getPath()));
            return JacksonUtils.toObject(FileUtils.readFileToString(infoFile, StandardCharsets.UTF_8), ItemInfo.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File getInfoFile(String itemName) {
        File infoFile = getFile(itemName, "info.json");
        return infoFile;
    }

    public static File getLogDir(String itemName) {
        File logDir = getFile(itemName, "log");
        return logDir;
    }

    public static File getFile(String itemName, String... fileNames) {
        String[] more = new String[fileNames.length + 1];
        more[0] = itemName;
        System.arraycopy(fileNames, 0, more, 1, fileNames.length);
        File file = Path.of(ITEMS_PATHNAME, more).toFile();
        return file;
    }

}

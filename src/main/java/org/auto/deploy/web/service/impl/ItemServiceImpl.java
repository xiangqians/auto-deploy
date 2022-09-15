package org.auto.deploy.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.auto.deploy.web.pojo.Item;
import org.auto.deploy.web.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author xiangqian
 * @date 21:26 2022/09/14
 */
@Slf4j
@Service
public class ItemServiceImpl implements ItemService {

    private File itemsDir;
    private Pattern pattern;

    @PostConstruct
    public void init() throws IOException {
        itemsDir = new File("items");
        FileUtils.forceMkdir(itemsDir);
        log.debug("itemsDir: {}", itemsDir.getAbsolutePath());

        pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");
    }

    @Override
    public Boolean modifyConfigFileContent(String itemName, String fileName, String content) throws IOException {
        File file = new File(String.format("items/%s/%s", itemName, fileName));
        Assert.isTrue(file.exists(), String.format("FileNotFoundException: %s", fileName));
        FileUtils.write(file, content, StandardCharsets.UTF_8);
        return true;
    }

    @Override
    public String getConfigFileContent(String itemName, String fileName) throws IOException {
        File file = new File(String.format("items/%s/%s", itemName, fileName));
        Assert.isTrue(file.exists(), String.format("FileNotFoundException: %s", fileName));
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    @Override
    public Boolean add(String name) throws IOException {
        Assert.isTrue(pattern.matcher(name).matches(), "预部署项目名称必须以字母开头，并且只能输入字母、数字、下划线、横杠，长度不能超过32个字符!");

        // config
        String configPath = String.format("items/%s/config", name);
        FileUtils.forceMkdir(new File(configPath));

        // config/core.yml
        String coreYmlContent = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResource("config/core.yml"), StandardCharsets.UTF_8);
        FileUtils.write(new File(String.format("%s/core.yml", configPath)), coreYmlContent, StandardCharsets.UTF_8);

        // config/jar/docker/Dockerfile
        String dockerPath = String.format("%s/jar/docker", configPath);
        FileUtils.forceMkdir(new File(dockerPath));
        String dockerfileContent = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResource("config/jar/docker/Dockerfile"), StandardCharsets.UTF_8);
        FileUtils.write(new File(String.format("%s/Dockerfile", dockerPath)), dockerfileContent, StandardCharsets.UTF_8);

        return true;
    }

    @Override
    public List<Item> queryForList() {
        Random random = new Random();
        List<Integer> statuses = List.of(Optional.of(random.nextInt(6)).filter(status -> status > 0).orElse(1),
                Optional.of(random.nextInt(6)).filter(status -> status > 0).orElse(1),
                Optional.of(random.nextInt(6)).filter(status -> status > 0).orElse(1),
                Optional.of(random.nextInt(6)).filter(status -> status > 0).orElse(1),
                Optional.of(random.nextInt(6)).filter(status -> status > 0).orElse(1));
        List<Item> items = Arrays.stream(itemsDir.listFiles())
                .map(file -> new Item(file.getName(), LocalDateTime.now(), "test1\ntest2\ntest3", statuses))
                .collect(Collectors.toList());
        return items;
    }

}

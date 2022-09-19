package org.auto.deploy.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.core.ItemService;
import org.auto.deploy.item.ItemInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xiangqian
 * @date 21:32 2022/09/13
 */
@Slf4j
@Controller
public class ItemController {

    @GetMapping("/item/{itemName}/log/{fileName}")
    public String deploymentHistoryLog(@PathVariable("itemName") String itemName,
                                       @PathVariable("fileName") String fileName,
                                       Model model) throws Exception {
        String content = ItemService.getFileContent(itemName, "log", fileName);
        model.addAttribute("title", String.format("%s/%s", itemName, fileName));
        model.addAttribute("content", content);
        return "log";
    }

    @ResponseBody
    @GetMapping("/item/{itemName}/log/list")
    public ResponseEntity<List<String>> logList(@PathVariable("itemName") String itemName) throws Exception {
        return new ResponseEntity<>(ItemService.getLogList(itemName), HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping("/item/{itemName}/deploy")
    public ResponseEntity<Boolean> deploy(@PathVariable("itemName") String itemName) throws Exception {
        return new ResponseEntity<>(ItemService.deploy(itemName), HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping("/item/{itemName}/config/jar/docker/{fileName}")
    public ResponseEntity<Boolean> modifyConfigFileContentForJarDockerXxx(@PathVariable("itemName") String itemName,
                                                                          @PathVariable("fileName") String fileName,
                                                                          @RequestBody String content) throws Exception {
        return new ResponseEntity<>(ItemService.modifyFileContent(itemName, new String[]{"config", "jar", "docker", fileName}, content), HttpStatus.OK);
    }

    @GetMapping("/item/{itemName}/config/jar/docker/{fileName}")
    public String getConfigFileContentForJarDockerXxx(@PathVariable("itemName") String itemName,
                                                      @PathVariable("fileName") String fileName,
                                                      Model model) throws Exception {
        return getConfigFileContent(itemName, new String[]{"jar", "docker", fileName}, "shell", model);
    }

    @ResponseBody
    @PostMapping("/item/{itemName}/config/{fileName}")
    public ResponseEntity<Boolean> modifyConfigFileContentForXxx(@PathVariable("itemName") String itemName,
                                                                 @PathVariable("fileName") String fileName,
                                                                 @RequestBody String content) throws Exception {
        return new ResponseEntity<>(ItemService.modifyFileContent(itemName, new String[]{"config", fileName}, content), HttpStatus.OK);
    }

    @GetMapping("/item/{itemName}/config/{fileName}")
    public String getConfigFileContentForXxx(@PathVariable("itemName") String itemName,
                                             @PathVariable("fileName") String fileName,
                                             Model model) throws Exception {
        return getConfigFileContent(itemName, new String[]{fileName}, "yaml", model);
    }

    public String getConfigFileContent(String itemName, String[] fileNames, String type, Model model) throws Exception {
        model.addAttribute("title", String.format("%s/%s", itemName, fileNames[fileNames.length - 1]));
        model.addAttribute("path", String.format("/item/%s/config/%s", itemName, StringUtils.join(fileNames, "/")));
        String[] more = new String[fileNames.length + 1];
        more[0] = "config";
        System.arraycopy(fileNames, 0, more, 1, fileNames.length);
        model.addAttribute("content", ItemService.getFileContent(itemName, more));
        model.addAttribute("type", type);
        return "editor";
    }

    @ResponseBody
    @PostMapping("/item/add/{name}")
    public ResponseEntity<Boolean> add(@PathVariable("name") String name) throws Exception {
        return new ResponseEntity<>(ItemService.add(name), HttpStatus.OK);
    }

    @GetMapping({"/", "/index", "/index.html"})
    public String index(Model model) throws Exception {
        List<ItemInfo> items = ItemService.getItemInfos();
        model.addAttribute("items", items);
        return "index";
    }

}

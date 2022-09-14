package org.auto.deploy.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.auto.deploy.web.pojo.Item;
import org.auto.deploy.web.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author xiangqian
 * @date 21:32 2022/09/13
 */
@Slf4j
@Controller
public class ItemController {

    @Autowired
    private ItemService itemService;

    @PostMapping("/item/{itemName}/config/jar/docker/{fileName}")
    public ResponseEntity<Boolean> modifyConfigFileContentFor1(@PathVariable("itemName") String itemName,
                                                               @PathVariable("fileName") String fileName,
                                                               @RequestBody String content) throws Exception {
        return new ResponseEntity<>(itemService.modifyConfigFileContent(itemName, String.format("config/jar/docker/%s", fileName), content), HttpStatus.OK);
    }

    @PostMapping("/item/{itemName}/config/{fileName}")
    public ResponseEntity<Boolean> modifyConfigFileContentFor0(@PathVariable("itemName") String itemName,
                                                               @PathVariable("fileName") String fileName,
                                                               @RequestBody String content) throws Exception {
        return new ResponseEntity<>(itemService.modifyConfigFileContent(itemName, String.format("config/%s", fileName), content), HttpStatus.OK);
    }

    @GetMapping("/item/{itemName}/config/jar/docker/{fileName}")
    public String getConfigFileContentFor1(@PathVariable("itemName") String itemName,
                                           @PathVariable("fileName") String fileName,
                                           Model model) throws Exception {
        return getConfigFileContent(itemName, "config/jar/docker", fileName, "shell", model);
    }

    @GetMapping("/item/{itemName}/config/{fileName}")
    public String getConfigFileContentFor0(@PathVariable("itemName") String itemName,
                                           @PathVariable("fileName") String fileName,
                                           Model model) throws Exception {
        return getConfigFileContent(itemName, "config", fileName, "yaml", model);
    }

    public String getConfigFileContent(String itemName, String fileBasePath, String fileName, String type, Model model) throws Exception {
        model.addAttribute("title", String.format("%s/%s", itemName, fileName));
        model.addAttribute("path", String.format("/item/%s/%s/%s", itemName, fileBasePath, fileName));
        model.addAttribute("content", itemService.getConfigFileContent(itemName, String.format("%s/%s", fileBasePath, fileName)));
        model.addAttribute("type", type);
        return "editor";
    }

    @PostMapping("/item/add/{name}")
    public ResponseEntity<Boolean> add(@PathVariable("name") String name) throws Exception {
        return new ResponseEntity<>(itemService.add(name), HttpStatus.OK);
    }

    @GetMapping({"/", "/index", "/index.html"})
    public String index(Model model) {
        List<Item> items = itemService.queryForList();
        model.addAttribute("items", items);
        return "index";
    }

}

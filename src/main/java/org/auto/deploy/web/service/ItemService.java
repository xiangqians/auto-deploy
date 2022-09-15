package org.auto.deploy.web.service;

import org.auto.deploy.web.pojo.Item;

import java.io.IOException;
import java.util.List;

/**
 * @author xiangqian
 * @date 21:26 2022/09/14
 */
public interface ItemService {

    // update
    Boolean modifyConfigFileContent(String itemName, String fileName, String content) throws IOException;

    String getConfigFileContent(String itemName, String fileName) throws IOException;

    // save
    Boolean add(String name) throws IOException;

    List<Item> queryForList();

}

package org.net.sftp;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 文件条目
 * drwxr-xr-x    2 root     root         4096 Jul  7 15:47 test
 * 解析：
 * type=d
 * properties=[rwx][r-x][r-x]
 * count=2
 * owner=root
 * group=root
 * size=4096
 * modifiedDate=Jul  7 15:47
 * name=test
 *
 * @author xiangqian
 * @date 01:38 2022/07/24
 */
@Data
public class FileEntry {

    private String type;
    private String properties;
    private Integer count;
    private String owner;
    private String group;
    private String size;
    private String modifiedDate;
    private String name;

    public static FileEntry parse(String str) {
        if (StringUtils.isEmpty(str = StringUtils.trim(str))) {
            return null;
        }

        String[] array = str.split("\\s+");
        if (array.length != 9) {
            return null;
        }

        FileEntry fileEntry = new FileEntry();
        fileEntry.setType(array[0].substring(0, 1));
        fileEntry.setProperties(array[0].substring(1));
        fileEntry.setCount(NumberUtils.toInt(array[1], -1));
        fileEntry.setOwner(array[2]);
        fileEntry.setGroup(array[3]);
        fileEntry.setSize(array[4]);
        fileEntry.setModifiedDate(StringUtils.join(array, " ", 5, 8));
        fileEntry.setName(array[8]);
        return fileEntry;
    }

}

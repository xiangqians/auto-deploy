package org.net.util;

import lombok.Getter;

/**
 * @author xiangqian
 * @date 01:01 2022/07/27
 */
@Getter
public enum FileExtension {
    ZIP("zip"),
    TAR_GZ("tar.gz"),
    JAR("jar"),
    ;

    private final String value;

    FileExtension(String value) {
        this.value = value;
    }

}

package org.auto.deploy.util;

/**
 * @author xiangqian
 * @date 13:39 2022/09/10
 */
public enum OS {
    WINDOWS,
    LINUX,
    ;

    public static OS get() {
        // 获取操作系统名称
        String osName = System.getProperty("os.name").toLowerCase();
        String osNameLowerCase = osName.toLowerCase();

        // windows
        if (osNameLowerCase.contains("windows")) {
            return WINDOWS;
        }

        // linux
        if (osNameLowerCase.contains("linux")) {
            return LINUX;
        }

        throw new UnknownError(String.format("目前暂不支持此操作系统: %s", osName));
    }

}

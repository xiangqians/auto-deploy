package org.auto.deploy.util;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author xiangqian
 * @date 23:53 2022/07/26
 */
public class FileUtils {

    // 扩展名	        文件头标识（HEX）	            文件描述
    // ZIP              50 4B 03
    // zip; jar; zipx	50 4B 03 04	                ZIP Archive
    // zip	            50 4B 30 30	                ZIP Archive (outdated)
    // Zip	            50 4B 30 30 50 4B 03 04	    WINZIP Compressed
    // gz; tar; tgz	    1F 8B	                    Gzip Archive File
    // gz; tgz	1F      8B 08	                    GZ Compressed File
    // JAR              4A 41 52 43 53 00	        JARCS compressed archive
    // jar              5F 27 A8 89	                JAR Archive File
    private static List<byte[]> ZIP_HEADERS = List.of(new byte[]{0x50, 0x4B, 0x03, 0x04});
    private static List<byte[]> TAR_GZ_HEADERS = List.of(new byte[]{0x1F, (byte) 0x8B});

    public static boolean isTarGzFile(File file) throws IOException {
        return isXxxFile(file, 2, bytes -> {
            for (byte[] tarGzHeader : TAR_GZ_HEADERS) {
                if (Arrays.equals(tarGzHeader, bytes)) {
                    return true;
                }
            }
            return false;
        });
    }

    public static boolean isZipFile(File file) throws IOException {
        return isXxxFile(file, 4, bytes -> {
            for (byte[] zipHeader : ZIP_HEADERS) {
                if (Arrays.equals(zipHeader, bytes)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean isXxxFile(File file, int length, Function<byte[], Boolean> function) throws IOException {
        if (Objects.isNull(file) || !file.exists() || file.isDirectory()) {
            return false;
        }

        InputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] buffer = new byte[length];
            int readLength = is.read(buffer, 0, length);
            if (readLength == length) {
                return function.apply(buffer);
            }
            return false;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}

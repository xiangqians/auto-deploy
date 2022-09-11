package org.auto.deploy.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.auto.deploy.util.CompressionUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author xiangqian
 * @date 10:17 2022/08/14
 */
public class CompressionUtilsTest {

    @Test
    public void tarGz() throws Exception {
        TarArchiveOutputStream tarOut = null;
        try {
            CompressionUtils.tarGz(new File[]{new File("C:\\Users\\xiangqian\\Desktop\\tmp\\compress-test\\t.txt"),
                    new File("C:\\Users\\xiangqian\\Desktop\\tmp\\compress-test\\test"),
                    new File("C:\\Users\\xiangqian\\Desktop\\tmp\\compress-test\\test.tar"),
                    new File("C:\\Users\\xiangqian\\Desktop\\tmp\\spring-boot-starter-tomcat-2.7.0.jar")
            }, new File("C:\\Users\\xiangqian\\Desktop\\tmp\\compress-test\\test.tar.gz"));
        } finally {
            IOUtils.closeQuietly(tarOut);
        }
    }

    @Test
    public void tar() throws Exception {
        TarArchiveOutputStream tarOut = null;
        try {
            File srcFile = new File("C:\\Users\\xiangqian\\Desktop\\tmp\\compress-test\\t.txt");
            tarOut = new TarArchiveOutputStream(new FileOutputStream("C:\\Users\\xiangqian\\Desktop\\tmp\\compress-test\\test.tar"));
            CompressionUtils.tar(new File[]{srcFile,
                    new File("C:\\Users\\xiangqian\\Desktop\\tmp\\compress-test\\test"),
                    new File("C:\\Users\\xiangqian\\Desktop\\tmp\\compress-test\\test.tar.gz")
            }, null, tarOut);
        } finally {
            IOUtils.closeQuietly(tarOut);
        }
    }

}

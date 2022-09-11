package org.auto.deploy.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Objects;
import java.util.UUID;

/**
 * https://commons.apache.org/proper/commons-compress/examples.html
 * 
 * @author xiangqian
 * @date 20:54 2022/08/11
 */
@Slf4j
public class CompressionUtils {

    public static void tarGz(File[] srcFiles, File destFile) throws IOException {
        TarArchiveOutputStream tarOut = null;
        InputStream tarIn = null;
        GzipCompressorOutputStream gzOut = null;
        try {
            // tar
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            tarOut = new TarArchiveOutputStream(byteArrayOut);
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tar(srcFiles, null, tarOut);
            tarIn = new ByteArrayInputStream(byteArrayOut.toByteArray());

            // gz
            gzOut = new GzipCompressorOutputStream(new FileOutputStream(destFile));
            gz(tarIn, gzOut);
        } finally {
            IOUtils.closeQuietly(tarIn, tarOut, gzOut);
        }
    }

    // 拷贝文件到目录：
    // org.apache.commons.io.FileUtils#copyFileToDirectory(java.io.File srcFile, java.io.File destDir)
    // 拷贝目录到目录：
    // org.apache.commons.io.FileUtils#copyDirectoryToDirectory(java.io.File sourceDir, java.io.File destinationDir)

    @Deprecated
    public static void tarGz(File srcFile, File destFile) throws IOException {
        File tarFile = null;
        TarArchiveOutputStream tarOut = null;
        try {
            String tempDirPath = FileUtils.getTempDirectoryPath();
            if (!tempDirPath.endsWith(File.separator)) {
                tempDirPath += File.separator;
            }
            log.debug("tempDirPath: {}", tempDirPath);
            tarFile = new File(tempDirPath + "temp_" + UUID.randomUUID().toString().replace("-", "") + ".tar");
            log.debug("tarFilePath: {}", tarFile.getAbsolutePath());

            // IllegalArgumentException: file name 'xxx ...' is too long ( > 100 bytes)
            // https://stackoverflow.com/questions/32528799/when-i-tar-a-file-its-throw-exception-as-is-too-long-100-bytes-tararchiveo
            // Look at this: https://commons.apache.org/proper/commons-compress/tar.html#Long_File_Names
            // You need to set format to posix before stream usage:
            // TarArchiveOutputStream stream = new TarArchiveOutputStream(...)
            // stream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            tarOut = new TarArchiveOutputStream(new FileOutputStream(tarFile));
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tar(srcFile, null, tarOut);

            // gz
            gz(tarFile, destFile);
        } finally {
            IOUtils.closeQuietly(tarOut);
            if (Objects.nonNull(tarFile)) {
                FileUtils.forceDelete(tarFile);
            }
        }
    }

    public static void unGz() {
    }

    public static void gz(File tarFile, File gzFile) throws IOException {
        GzipCompressorOutputStream gzOut = null;
        try {
            gzOut = new GzipCompressorOutputStream(new FileOutputStream(gzFile));
            gz(tarFile, gzOut);
        } finally {
            IOUtils.closeQuietly(gzOut);
        }
    }

    /**
     * gz
     *
     * @param tarFile
     * @param gzOut
     * @throws IOException
     */
    public static void gz(File tarFile, GzipCompressorOutputStream gzOut) throws IOException {
        InputStream tarIn = null;
        try {
            tarIn = new FileInputStream(tarFile);
            gz(tarIn, gzOut);
        } finally {
            IOUtils.closeQuietly(tarIn);
        }
    }

    /**
     * gz
     *
     * @param tarIn
     * @param gzOut
     * @throws IOException
     */
    public static void gz(InputStream tarIn, GzipCompressorOutputStream gzOut) throws IOException {
        IOUtils.copy(tarIn, gzOut);
    }

    public static void tar(File[] srcFiles, String base, TarArchiveOutputStream tarOut) throws IOException {
        for (File srcFile : srcFiles) {
            tar(srcFile, base, tarOut);
        }
    }

    /**
     * tar
     *
     * @param srcFile 要添加到归档的本地文件夹或者文件
     * @param base    .tar压缩包中的基本路径
     * @param tarOut  tar归档输出流
     */
    public static void tar(File srcFile, String base, TarArchiveOutputStream tarOut) throws IOException {
        if (StringUtils.isEmpty(base = StringUtils.trimToNull(base))) {
            base = srcFile.getName();
        } else {
            base = base + File.separator + srcFile.getName();
        }

        TarArchiveEntry tarEntry = new TarArchiveEntry(srcFile, base);
        tarOut.putArchiveEntry(tarEntry);

        if (srcFile.isFile()) {
            InputStream in = null;
            try {
                in = new FileInputStream(srcFile);
                IOUtils.copy(in, tarOut);
            } finally {
                IOUtils.closeQuietly(in);
            }
            tarOut.closeArchiveEntry();
            return;
        }

        tarOut.closeArchiveEntry();
        File[] children = srcFile.listFiles();
        if (ArrayUtils.isNotEmpty(children)) {
            for (File child : children) {
                tar(child, base, tarOut);
            }
        }
    }

}

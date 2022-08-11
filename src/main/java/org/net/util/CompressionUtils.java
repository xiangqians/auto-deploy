package org.net.util;

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
 * <p>
 * Apache Commons Compress是一个压缩、解压缩文件的类库。
 * 可以操作ar, cpio, Unix dump, tar, zip, gzip, XZ, Pack200 and bzip2格式的文件，功能比较强大。
 * <p>
 * tar是一种很老的归档格式，最早可以追溯到UNIX时代，tar已成为POSIX标准之一。tar早期经常用于将文件拷贝到磁带上（这里的磁带可不是以前听歌用的磁带，而是早期计算机的存储截至），加上tar格式诞生时间很早，因此tar标准里面的很多设计今天看起来很奇怪。
 * 对于tar格式，需要注意的是，它是一个“归档”（archive）格式。何谓归档？简单的说，就是把“散落”的文件都整齐的“码放”在一起。换句话说，tar格式只是单纯的把文件放在一起，并不能起到压缩的效果。常见的压缩格式如tar.gz，tar.bz2，tar.xz 等格式，其实是把文件通过tar放在一起之后，用gzip、bzip2、xz等工具再进行一次压缩得到的。
 * 另外，tar格式的设计与zip、7zip等常见压缩格式有些不同-它没有类似“目录”的存在，这就导致很难做到zip、7zip一样只解压特定的文件（这种操作一般称作随机访问random access）而不去碰解压其他文件。Commons Compress对于tar格式也是这样设计的：它不提供类似ZipFile这样可以罗列所有文件的类和方法，只能按照文件在tar中的顺序去逐个遍历。
 *
 * @author xiangqian
 * @date 20:54 2022/08/11
 */
@Slf4j
public class CompressionUtils {

    public static void tarGz(File srcFile, File destFile) throws IOException {
        File tarFile = null;
        TarArchiveOutputStream tarOut = null;
        GzipCompressorOutputStream gzOut = null;
        try {
            String tempDirPath = FileUtils.getTempDirectoryPath();
            log.debug("tempDirPath: {}", tempDirPath);
            tarFile = new File(tempDirPath + File.separator + "temp_" + UUID.randomUUID().toString().replace("-", "") + ".tar");
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
            gzOut = new GzipCompressorOutputStream(new FileOutputStream(destFile));
            gz(tarFile, gzOut);
        } finally {
            IOUtils.closeQuietly(tarOut, gzOut);
            if (Objects.nonNull(tarFile)) {
                FileUtils.forceDelete(tarFile);
            }
        }
    }

    public static void unGz() {
    }

    /**
     * gz
     *
     * @param tarFile
     * @param gzOut
     * @throws IOException
     */
    public static void gz(File tarFile, GzipCompressorOutputStream gzOut) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(tarFile);
            IOUtils.copy(in, gzOut);
        } finally {
            IOUtils.closeQuietly(in);
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

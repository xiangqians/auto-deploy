package org.net.sftp;

import com.jcraft.jsch.SftpProgressMonitor;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * SFTP
 *
 * @author xiangqian
 * @date 00:04 2022/07/24
 */
public interface Sftp extends Closeable {

    /**
     * 查询指定目录下的文件列表
     *
     * @param path
     * @return
     * @throws Exception
     */
    List<FileEntry> ls(String path) throws Exception;

    /**
     * 进入指定目录
     *
     * @param path
     * @throws Exception
     */
    void cd(String path) throws Exception;

    /**
     * 创建目录
     *
     * @param path
     */
    void mkdir(String path) throws Exception;

    /**
     * 删除目录（只允许删除空目录）
     *
     * @param path
     * @throws Exception
     */
    void rmdir(String path) throws Exception;

    /**
     * 删除指定文件
     *
     * @param path
     * @throws Exception
     */
    void rm(String path) throws Exception;

    /**
     * 重命名（移动）指定文件或目录
     *
     * @param oldPath
     * @param newPath
     * @throws Exception
     */
    void rename(String oldPath, String newPath) throws Exception;

    /**
     * 文件上传
     *
     * @param src     本地源文件
     * @param dst     linux远程服务器源文件
     * @param monitor Sftp传输进度监视器
     * @param mode    文件传输模式
     * @throws Exception
     */
    void put(String src, String dst, SftpProgressMonitor monitor, FileTransferMode mode) throws Exception;

    /**
     * 文件上传
     *
     * @param src     本地文件输入流
     * @param dst     linux远程服务器源文件
     * @param monitor Sftp传输进度监视器
     * @param mode    文件传输模式
     * @throws Exception
     */
    void put(InputStream src, String dst, SftpProgressMonitor monitor, FileTransferMode mode) throws Exception;

    /**
     * 文件下载
     *
     * @param src     linux远程服务器上的源文件
     * @param dst     输出流
     * @param monitor Sftp传输进度监视器
     * @param mode    文件传输模式
     */
    void get(String src, OutputStream dst, SftpProgressMonitor monitor, FileTransferMode mode) throws Exception;

    /**
     * 文件下载
     *
     * @param src     linux远程服务器上的源文件
     * @param dst     本地目标文件
     * @param monitor Sftp传输进度监视器
     * @param mode    文件传输模式
     */
    void get(String src, String dst, SftpProgressMonitor monitor, FileTransferMode mode) throws Exception;

}

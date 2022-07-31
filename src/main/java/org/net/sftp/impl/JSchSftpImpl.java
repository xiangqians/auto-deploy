package org.net.sftp.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpProgressMonitor;
import lombok.extern.slf4j.Slf4j;
import org.net.sftp.FileEntry;
import org.net.sftp.FileTransferMode;
import org.net.sftp.Sftp;
import org.net.ssh.JSchSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xiangqian
 * @date 01:13 2022/07/24
 */
@Slf4j
public class JSchSftpImpl extends JSchSupport implements Sftp {

    private ChannelSftp channel;

    private JSchSftpImpl() {
    }

    @Override
    public List<FileEntry> ls(String path) throws Exception {
        initChannelIfNotInitialized();
        Vector<?> vector = channel.ls(path);
        return Optional.ofNullable(vector)
                .map(objects -> objects.stream().map(object -> FileEntry.parse(object.toString())).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public void cd(String path) throws Exception {
        initChannelIfNotInitialized();
        channel.cd(path);
    }

    @Override
    public void mkdir(String path) throws Exception {
        initChannelIfNotInitialized();
        channel.mkdir(path);
    }

    @Override
    public void rmdir(String path) throws Exception {
        initChannelIfNotInitialized();
        channel.rmdir(path);
    }

    @Override
    public void rm(String path) throws Exception {
        initChannelIfNotInitialized();
        channel.rm(path);
    }

    @Override
    public void rename(String oldPath, String newPath) throws Exception {
        initChannelIfNotInitialized();
        channel.rename(oldPath, newPath);
    }

    @Override
    public void put(String src, String dst, SftpProgressMonitor monitor, FileTransferMode mode) throws Exception {
        initChannelIfNotInitialized();
        channel.put(src, dst, monitor, mode.getValue());
    }

    @Override
    public void put(InputStream src, String dst, SftpProgressMonitor monitor, FileTransferMode mode) throws Exception {
        initChannelIfNotInitialized();
        channel.put(src, dst, monitor, mode.getValue());
    }

    @Override
    public void get(String src, OutputStream dst, SftpProgressMonitor monitor, FileTransferMode mode) throws Exception {
        initChannelIfNotInitialized();
        long skip = 0;
        channel.get(src, dst, monitor, mode.getValue(), skip);
    }

    @Override
    public void get(String src, String dst, SftpProgressMonitor monitor, FileTransferMode mode) throws Exception {
        initChannelIfNotInitialized();
        channel.get(src, dst, monitor, mode.getValue());
    }

    private void initChannelIfNotInitialized() throws JSchException {
        if (Objects.isNull(channel)) {
            synchronized (this) {
                if (Objects.isNull(channel)) {
                    ChannelSftp channel = openSftpChannel();
                    connectChannel(channel);
                    this.channel = channel;
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            close(channel);
            channel = null;
        } finally {
            super.close();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends JSchSupport.Builder<Builder, JSchSftpImpl> {
        private Builder() {
        }

        @Override
        protected JSchSftpImpl get() {
            return new JSchSftpImpl();
        }
    }

}

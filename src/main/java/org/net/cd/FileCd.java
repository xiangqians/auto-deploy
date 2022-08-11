package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.net.sftp.FileTransferMode;
import org.net.sftp.impl.DefaultSftpProgressMonitor;
import org.net.util.Assert;
import org.net.util.CompressionUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * File持续部署
 *
 * @author xiangqian
 * @date 23:51 2022/07/26
 */
@Slf4j
public class FileCd extends AbstractCd {

    // file
    private File file;
    // 是否清空工作目录
    private boolean clearWorkDir;
    // .tar.gz
    private String tarGzFileName;
    private File tarGzFile;

    /**
     * 压缩要上传的文件或文件夹
     */
    private void compress() throws IOException {
        log.debug("准备压缩本地文件或文件夹({}) ...", file.getAbsolutePath());
        tarGzFileName = String.format("%s.tar.gz", UUID.randomUUID().toString().replace("-", ""));
        tarGzFile = new File(tarGzFileName);
        CompressionUtils.tarGz(file, tarGzFile);
        log.debug("已压缩本地文件或文件夹! (压缩文件为 {})", tarGzFile.getAbsolutePath());
    }

    /**
     * 清空当前工作目录
     *
     * @throws Exception
     */
    private void clearWorkDir() throws Exception {
        log.debug("准备清空当前工作目录({}) ...", workDir);
        String cmd = String.format("rm -rf ./../%s/*", workDir);
        ssh.execute(cmd, resultConsumer(cmd));
        log.debug("已清空当前工作目录!");
    }

    /**
     * 上传压缩包
     *
     * @throws Exception
     */
    private void upload() throws Exception {
        log.debug("准备上传压缩文件({}) ...", tarGzFile.getAbsolutePath());
        sftp.put(tarGzFile.getAbsolutePath(), tarGzFileName, DefaultSftpProgressMonitor.builder().build(), FileTransferMode.OVERWRITE);
        log.debug("已上传压缩文件!");
    }

    /**
     * 解压
     *
     * @throws Exception
     */
    private void decompress() throws Exception {
        log.debug("准备解压文件({}) ...", tarGzFileName);
        String cmd = String.format("tar -zxvf ./%s", tarGzFileName);
        ssh.execute(cmd, Duration.ofMinutes(5), resultConsumer(cmd));
        log.debug("已解压文件!");
    }

    /**
     * 删除压缩包
     *
     * @throws Exception
     */
    private void deleteArchive() throws Exception {
        log.debug("准备删除压缩文件({}) ...", tarGzFileName);
        String cmd = String.format("rm -rf ./%s", tarGzFileName);
        ssh.execute(cmd, resultConsumer(cmd));
        log.debug("已删除压缩文件!");
    }

    @Override
    public void execute() throws Exception {
        try {
            compress();
            cdWorkDir();
            if (clearWorkDir) {
                clearWorkDir();
            }
            upload();
            decompress();
            deleteArchive();
        } finally {
            if (Objects.nonNull(tarGzFile)) {
                FileUtils.forceDelete(tarGzFile);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, FileCd> {
        private String filePath;
        private boolean clearWorkDir;

        private Builder() {
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder clearWorkDir(boolean clearWorkDir) {
            this.clearWorkDir = clearWorkDir;
            return this;
        }

        @Override
        protected FileCd get() {
            return new FileCd();
        }

        @Override
        public FileCd build() throws Exception {
            Assert.notNull(workDir, "workDir不能为空");
            // clearWorkDir
            if (clearWorkDir) {
                while (true) {
                    System.out.format("此配置将会清空Linux服务器工作目录（%s）下的所有文件 [y/N]: ", workDir);
                    int input = System.in.read();
                    System.in.skip(System.in.available());
                    if ('y' == input) {
                        break;
                    }

                    if ('N' == input) {
                        clearWorkDir = false;
                        break;
                    }
                }
            }
            log.debug("clearWorkDir: {}", clearWorkDir);

            log.debug("filePath: {}", filePath);
            Assert.notNull(filePath, "filePath不能为空");
            File file = new File(filePath);
            Assert.isTrue(file.exists(), String.format("%s 文件或文件夹不存在", filePath));

            FileCd fileCd = super.build();
            fileCd.file = file;
            fileCd.clearWorkDir = clearWorkDir;
            return fileCd;
        }
    }

}

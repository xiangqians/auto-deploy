package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.net.sftp.FileTransferMode;
import org.net.sftp.impl.DefaultSftpProgressMonitor;
import org.net.util.Assert;
import org.net.util.FileExtension;
import org.net.util.FileUtils;

import java.io.File;
import java.time.Duration;
import java.util.List;

/**
 * 静态资源持续部署
 *
 * @author xiangqian
 * @date 23:51 2022/07/26
 */
@Slf4j
public class StaticWebCd extends AbstractCd {

    private File file;
    // 文件全名，包括文件名和后缀名
    private String fileFullName;
    // 文件名
    private String fileName;
    // 文件后缀
    private FileExtension fileExtension;
    // 是否清空工作目录
    private boolean clearWorkDir;

    /**
     * 清空当前工作目录
     *
     * @throws Exception
     */
    public void clearWorkDir() throws Exception {
        String cmd = String.format("rm -rf ./../%s/*", workDir);
        ssh.execute(cmd, resultConsumer(cmd));
    }

    /**
     * 上传压缩包
     *
     * @throws Exception
     */
    public void uploadArchive() throws Exception {
        sftp.put(file.getAbsolutePath(), fileFullName, DefaultSftpProgressMonitor.builder().build(), FileTransferMode.OVERWRITE);
    }

    /**
     * 解压
     *
     * @throws Exception
     */
    public void decompress() throws Exception {
        String cmd = null;
        switch (fileExtension) {
            case ZIP:
                // According to http://www.manpagez.com/man/1/unzip/ you can use the -o option to overwrite files:
                // unzip -o /path/to/archive.zip
                // Note that -o, like most of unzip's options, has to go before the archive name.
                cmd = String.format("unzip -o ./%s", fileFullName);
                break;

            case TAR_GZ:
                cmd = String.format("tar -zxvf ./%s", fileFullName);
                break;

            default:
                throw new UnsupportedOperationException();
        }

        ssh.execute(cmd, Duration.ofMinutes(5), resultConsumer(cmd));
    }

    /**
     * 删除压缩包
     *
     * @throws Exception
     */
    public void deleteArchive() throws Exception {
        String cmd = String.format("rm -rf ./%s", fileFullName);
        ssh.execute(cmd, resultConsumer(cmd));
    }

    @Override
    public void execute() throws Exception {
        cdWorkDir();
        if (clearWorkDir) {
            clearWorkDir();
        }
        uploadArchive();
        decompress();
        deleteArchive();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractCd.Builder<Builder, StaticWebCd> {
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
        protected StaticWebCd get() {
            return new StaticWebCd();
        }

        @Override
        public StaticWebCd build() throws Exception {
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
            Assert.isTrue(file.exists(), String.format("%s 文件不存在", filePath));
            Assert.isTrue(!file.isDirectory(), String.format("%s 必须是文件", filePath));
            FileExtension fileExtension = null;
            if (FileUtils.isZipFile(file)) {
                fileExtension = FileExtension.ZIP;
            } else if (FileUtils.isTarGzFile(file)) {
                fileExtension = FileExtension.TAR_GZ;
            }
            Assert.notNull(fileExtension, String.format("%s 无法解析此文件类型，目前只支持 zip, tar.gz", filePath));
            log.debug("fileExtension: {}", fileExtension);

            // fileFullName
            int index = filePath.lastIndexOf(File.separator);
            String fileFullName = filePath.substring(index + 1);
            log.debug("fileFullName: {}", fileFullName);
            // fileName
            String fileName = fileFullName.replace(String.format(".%s", fileExtension.getValue()), "");
            log.debug("fileName: {}", fileName);

            StaticWebCd staticWebCd = super.build();
            staticWebCd.file = file;
            staticWebCd.fileFullName = fileFullName;
            staticWebCd.fileName = fileName;
            staticWebCd.fileExtension = fileExtension;
            staticWebCd.clearWorkDir = clearWorkDir;

            return staticWebCd;
        }

    }

}

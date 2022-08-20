package org.net.cd.source;

import org.apache.commons.lang3.ArrayUtils;
import org.net.cd.AbstractCd;
import org.net.util.Assert;

import java.io.File;
import java.io.IOException;

/**
 * 文件资源
 *
 * @author xiangqian
 * @date 23:55 2022/08/19
 */
public class FileSource implements Source {

    private File[] files;

    private FileSource(File[] files) {
        this.files = files;
    }

    @Override
    public File[] get() throws Exception {
        return files;
    }

    @Override
    public void close() throws IOException {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String[] filePaths;

        private Builder() {
        }

        public Builder filePaths(String... filePaths) {
            this.filePaths = filePaths;
            return this;
        }

        public FileSource build() throws Exception {
            Assert.notNull(filePaths, "filePaths不能为空");
            return new FileSource(AbstractCd.getFiles(filePaths));
        }

    }
}

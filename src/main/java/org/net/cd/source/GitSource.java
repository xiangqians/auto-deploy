package org.net.cd.source;

import java.io.File;
import java.io.IOException;

/**
 * @author xiangqian
 * @date 12:10 2022/08/20
 */
public class GitSource implements Source {

    private GitSource() {
    }

    @Override
    public File[] get() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

}

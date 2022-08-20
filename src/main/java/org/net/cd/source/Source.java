package org.net.cd.source;

import java.io.Closeable;
import java.io.File;

/**
 * 资源
 *
 * @author xiangqian
 * @date 23:50 2022/08/19
 */
public interface Source extends Closeable {

    /**
     * 获取资源
     *
     * @return
     */
    File[] get() throws Exception;

}

package org.auto.deploy.support.source;

import java.io.Closeable;
import java.io.File;

/**
 * 资源
 *
 * @author xiangqian
 * @date 01:30 2022/09/10
 */
public interface Source extends Closeable {

    /**
     * 获取资源
     *
     * @return
     * @throws Exception
     */
    File get() throws Exception;

}

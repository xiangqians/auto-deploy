package org.net.cd;

import java.io.Closeable;

/**
 * @author xiangqian
 * @date 01:21 2022/07/26
 */
public interface Cd extends Closeable {

    void execute() throws Exception;

}

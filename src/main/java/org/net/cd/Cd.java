package org.net.cd;

import java.io.Closeable;

/**
 * 持续部署
 *
 * @author xiangqian
 * @date 01:21 2022/07/26
 */
public interface Cd extends Closeable {

    void execute() throws Exception;

}

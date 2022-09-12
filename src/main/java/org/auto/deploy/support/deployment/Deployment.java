package org.auto.deploy.support.deployment;

import java.io.Closeable;

/**
 * 部署
 *
 * @author xiangqian
 * @date 12:41 2022/09/10
 */
public interface Deployment extends Closeable {

    void deploy() throws Exception;

}

package org.auto.deploy;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xiangqian
 * @date 10:44 2022/09/12
 */
@Slf4j
public class AutoDeployApplicationTest {

    public static void main(String[] args) throws Exception {
        String configLocation = "C:\\Users\\xiangqian\\Desktop\\tmp\\auto-deploy-config\\config.yml";
        System.setProperty("auto.deploy.config.location", configLocation);
        AutoDeployApplication.main(args);
    }

}

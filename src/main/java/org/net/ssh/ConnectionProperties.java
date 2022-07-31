package org.net.ssh;

import lombok.Data;

/**
 * @author xiangqian
 * @date 13:12 2022/07/23
 */
@Data
public class ConnectionProperties {

    private String host;
    private int port;
    private String username;
    private String password;

}

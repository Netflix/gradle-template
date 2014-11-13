package io.reactivex.lab.services;

import com.netflix.eureka2.registry.datacenter.LocalDataCenterInfo;
import com.netflix.eureka2.server.EurekaWriteServer;
import com.netflix.eureka2.server.WriteServerConfig;

/**
 * @author Nitesh Kant
 */
public class StartEurekaServer {

    public static final int READ_SERVER_PORT = 7001;
    public static final int WRITE_SERVER_PORT = 7002;

    public static void main(String[] args) throws Exception {
        WriteServerConfig.WriteServerConfigBuilder builder = new WriteServerConfig.WriteServerConfigBuilder();
        builder.withReadServerPort(READ_SERVER_PORT).withWriteServerPort(WRITE_SERVER_PORT)
               .withWriteClusterAddresses(new String[]{"127.0.0.1"})
               .withDataCenterType(LocalDataCenterInfo.DataCenterType.Basic);
        EurekaWriteServer eurekaWriteServer = new EurekaWriteServer(builder.build());
        eurekaWriteServer.start();
        eurekaWriteServer.waitTillShutdown();
    }
}

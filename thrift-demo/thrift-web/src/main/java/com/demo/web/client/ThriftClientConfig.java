package com.demo.web.client;

import com.demo.thrift.ReconciliationService;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.springframework.stereotype.Component;

@Component
public class ThriftClientConfig {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9090;

    public ReconciliationService.Client getClient() throws Exception {
        TTransport transport = new TSocket(HOST, PORT);
        transport.open();
        return new ReconciliationService.Client(new TBinaryProtocol(transport));
    }
}
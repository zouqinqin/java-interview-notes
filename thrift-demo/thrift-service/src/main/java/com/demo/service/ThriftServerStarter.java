package com.demo.service;

import com.demo.service.impl.ReconciliationServiceImpl;
import com.demo.thrift.ReconciliationService;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.protocol.TBinaryProtocol;

public class ThriftServerStarter {

    private static final int PORT = 9090;

    public static void main(String[] args) throws Exception {
        ReconciliationServiceImpl serviceImpl = new ReconciliationServiceImpl();

        ReconciliationService.Processor<ReconciliationServiceImpl> processor =
                new ReconciliationService.Processor<>(serviceImpl);

        TServerSocket serverTransport = new TServerSocket(PORT);

        TSimpleServer.Args serverArgs = new TSimpleServer.Args(serverTransport)
                .processor(processor)
                .protocolFactory(new TBinaryProtocol.Factory());

        TServer server = new TSimpleServer(serverArgs);
        System.out.println("=== Thrift Server 启动，端口: " + PORT + " ===");
        server.serve();
    }
}
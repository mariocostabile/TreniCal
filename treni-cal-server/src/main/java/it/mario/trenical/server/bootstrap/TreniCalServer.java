package it.mario.trenical.server.bootstrap;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import it.mario.trenical.server.api.grpc.TreniCalServiceImpl;

public class TreniCalServer {

    private Server server;

    private void start() throws Exception {
        int port = 50051;
        server = ServerBuilder
                .forPort(port)
                .addService(new TreniCalServiceImpl())
                .build()
                .start();

        System.out.println("[TreniCalServer] gRPC in ascolto su localhost:" + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[TreniCalServer] Shutdown hook...");
            TreniCalServer.this.stop();
            System.out.println("[TreniCalServer] Server arrestato.");
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        TreniCalServer s = new TreniCalServer();
        s.start();
        s.blockUntilShutdown();
    }
}

package it.mario.trenical.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import it.mario.trenical.server.api.grpc.TreniCalServiceImpl;

import java.io.IOException;
import java.util.logging.Logger;

public class ServerMain {

    private static final Logger log = Logger.getLogger(ServerMain.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051; // porta default

        Server server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new TreniCalServiceImpl())
                .build();

        log.info(() -> "🚂 TreniCalServer avviato sulla porta " + port);
        server.start();

        // shutdown hook per kill pulito
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("👉 Shutdown richiesto, chiusura server...");
            server.shutdown();
        }));

        server.awaitTermination();
    }
}

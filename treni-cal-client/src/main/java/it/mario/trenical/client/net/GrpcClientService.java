package it.mario.trenical.client.net;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import it.mario.trenical.proto.*;

import java.time.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcClientService {

    private final ManagedChannel channel;
    private final TreniCalServiceGrpc.TreniCalServiceBlockingStub blocking;
    private final TreniCalServiceGrpc.TreniCalServiceStub async;

    public GrpcClientService(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blocking = TreniCalServiceGrpc.newBlockingStub(channel);
        this.async = TreniCalServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown();
        channel.awaitTermination(3, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        GrpcClientService client = new GrpcClientService("localhost", 50051);

        try {
            // 1) SearchStations("Roma")
            System.out.println("== SearchStations(\"Roma\") ==");
            SearchStationsResponse sresp = client.blocking.searchStations(
                    SearchStationsRequest.newBuilder().setQuery("Roma").build()
            );
            for (Station s : sresp.getStationsList()) {
                System.out.printf("- %s [%s] (%s)%n", s.getName(), s.getCode(), s.getCity());
            }

            // 2) SearchTrains("RMT","MCE","2025-08-26")  -> usa Timestamp su departure_after
            System.out.println("\n== SearchTrains(\"RMT\",\"MCE\",\"2025-08-26\") ==");
            SearchTrainsResponse tresp = client.blocking.searchTrains(
                    SearchTrainsRequest.newBuilder()
                            .setOriginCode("RMT")
                            .setDestinationCode("MCE")
                            .setDepartureAfter(tsFromLocalDateIso("2025-08-26"))
                            .build()
            );
            for (Train t : tresp.getTrainsList()) {
                System.out.printf("- %s %s -> %s  (%s → %s)%n",
                        t.getCode(), t.getOriginName(), t.getDestinationName(),
                        tsToIso(t.getDepartureTime()), tsToIso(t.getArrivalTime()));
            }

            // 3) SubscribeTrainStatus("T_FR_9002") -> usa event_time
            System.out.println("\n== SubscribeTrainStatus(\"T_FR_9002\") ==");
            CountDownLatch done = new CountDownLatch(1);
            asyncSubscribe(client, "T_FR_9002", done);

            // Attendo gli update mock (3 eventi ~2.5s totali)
            done.await(5, TimeUnit.SECONDS);

        } finally {
            client.shutdown();
        }
    }

    private static void asyncSubscribe(GrpcClientService client, String trainCode, CountDownLatch done) {
        SubscribeTrainStatusRequest req = SubscribeTrainStatusRequest.newBuilder()
                .setTrainCode(trainCode)
                .build();

        client.async.subscribeTrainStatus(req, new StreamObserver<TrainStatusUpdate>() {
            @Override
            public void onNext(TrainStatusUpdate u) {
                System.out.printf("UPDATE %s | delay=%d' | platform=%s | status=%s | ts=%s%n",
                        u.getTrainCode(),
                        u.getDelayMinutes(),
                        u.getPlatform(),
                        u.getStatus().name(),
                        tsToIso(u.getEventTime()));
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Streaming error: " + t.getMessage());
                done.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Streaming completed.");
                done.countDown();
            }
        });
    }

    // ===== Utilities Timestamp <-> ISO =====

    /** Converte "YYYY-MM-DD" in Timestamp (00:00:00 UTC di quel giorno). */
    private static Timestamp tsFromLocalDateIso(String yyyyMmDd) {
        LocalDate d = LocalDate.parse(yyyyMmDd);
        Instant inst = d.atStartOfDay().toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder()
                .setSeconds(inst.getEpochSecond())
                .setNanos(inst.getNano())
                .build();
    }

    /** Converte Timestamp in stringa ISO-8601 (UTC). */
    private static String tsToIso(Timestamp ts) {
        if (ts == null) return "";
        return OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()),
                ZoneOffset.UTC
        ).toString();
    }
}

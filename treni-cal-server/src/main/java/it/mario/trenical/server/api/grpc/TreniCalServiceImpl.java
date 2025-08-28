package it.mario.trenical.server.api.grpc;

import io.grpc.stub.StreamObserver;
import it.mario.trenical.proto.*;
import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TreniCalServiceImpl extends TreniCalServiceGrpc.TreniCalServiceImplBase {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void searchStations(SearchStationsRequest request, StreamObserver<SearchStationsResponse> responseObserver) {
        String q = request.getQuery() == null ? "" : request.getQuery().toLowerCase(Locale.ITALY);

        Station romaTermini = Station.newBuilder()
                .setId("ST_RMT")
                .setName("Roma Termini")
                .setCode("RMT")
                .setCity("Roma")
                .build();

        Station milanoCentrale = Station.newBuilder()
                .setId("ST_MCE")
                .setName("Milano Centrale")
                .setCode("MCE")
                .setCity("Milano")
                .build();

        SearchStationsResponse.Builder resp = SearchStationsResponse.newBuilder();
        if (q.contains("roma")) resp.addStations(romaTermini);
        if (q.contains("milano")) resp.addStations(milanoCentrale);
        if (q.isBlank()) resp.addAllStations(List.of(romaTermini, milanoCentrale));

        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    @Override
    public void searchTrains(SearchTrainsRequest request, StreamObserver<SearchTrainsResponse> responseObserver) {
        // Mock: un solo treno RMT -> MCE
        Train tr = Train.newBuilder()
                .setId("TR_9002")
                .setCode("T_FR_9002")
                .setOriginCode(request.getOriginCode().isBlank() ? "RMT" : request.getOriginCode())
                .setOriginName("Roma Termini")
                .setDestinationCode(request.getDestinationCode().isBlank() ? "MCE" : request.getDestinationCode())
                .setDestinationName("Milano Centrale")
                .setDepartureTime(tsFromIso("2025-08-26T05:30:00Z"))
                .setArrivalTime(tsFromIso("2025-08-26T08:30:00Z"))
                .build();

        SearchTrainsResponse resp = SearchTrainsResponse.newBuilder()
                .addTrains(tr)
                .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void purchaseTicket(PurchaseTicketRequest request, StreamObserver<PurchaseTicketResponse> responseObserver) {
        int qty = Math.max(1, request.getQuantity());
        PurchaseTicketResponse.Builder resp = PurchaseTicketResponse.newBuilder();

        for (int i = 1; i <= qty; i++) {
            Ticket t = Ticket.newBuilder()
                    .setId(String.format("TCK-%04d", i))
                    .setTrainId(request.getTrainId().isBlank() ? "TR_9002" : request.getTrainId())
                    .setPassengerName(request.getPassengerName().isBlank() ? "Mario Rossi" : request.getPassengerName())
                    .setClassOfService(request.getClassOfService().isBlank() ? "STANDARD" : request.getClassOfService())
                    .setPurchaseDate(tsNow())
                    .build();
            resp.addTickets(t);
        }

        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    @Override
    public void cancelTicket(CancelTicketRequest request, StreamObserver<CancelTicketResponse> responseObserver) {
        CancelTicketResponse resp = CancelTicketResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Cancelled ticket " + request.getTicketId())
                .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void subscribeTrainStatus(SubscribeTrainStatusRequest request, StreamObserver<TrainStatusUpdate> responseObserver) {
        String trainCode = request.getTrainCode().isBlank() ? "T_FR_9002" : request.getTrainCode();

        TrainStatusUpdate u1 = TrainStatusUpdate.newBuilder()
                .setTrainCode(trainCode)
                .setDelayMinutes(0)
                .setPlatform("5")
                .setStatus(TrainStatus.TRAIN_STATUS_ON_TIME)
                .setEventTime(tsNow())
                .build();

        TrainStatusUpdate u2 = TrainStatusUpdate.newBuilder()
                .setTrainCode(trainCode)
                .setDelayMinutes(15)
                .setPlatform("5")
                .setStatus(TrainStatus.TRAIN_STATUS_DELAYED)
                .setEventTime(tsPlusSeconds(60))
                .build();

        TrainStatusUpdate u3 = TrainStatusUpdate.newBuilder()
                .setTrainCode(trainCode)
                .setDelayMinutes(10)
                .setPlatform("6")
                .setStatus(TrainStatus.TRAIN_STATUS_DELAYED)
                .setEventTime(tsPlusSeconds(120))
                .build();

        scheduler.schedule(() -> responseObserver.onNext(u1), 500, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> responseObserver.onNext(u2), 1500, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> {
            responseObserver.onNext(u3);
            responseObserver.onCompleted();
        }, 2500, TimeUnit.MILLISECONDS);
    }

    private static Timestamp tsNow() {
        Instant now = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        return Timestamp.newBuilder().setSeconds(now.getEpochSecond()).setNanos(now.getNano()).build();
    }

    private static Timestamp tsPlusSeconds(int seconds) {
        Instant inst = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(seconds).toInstant();
        return Timestamp.newBuilder().setSeconds(inst.getEpochSecond()).setNanos(inst.getNano()).build();
    }

    private static Timestamp tsFromIso(String iso) {
        Instant inst = Instant.parse(iso);
        return Timestamp.newBuilder().setSeconds(inst.getEpochSecond()).setNanos(inst.getNano()).build();
    }
}

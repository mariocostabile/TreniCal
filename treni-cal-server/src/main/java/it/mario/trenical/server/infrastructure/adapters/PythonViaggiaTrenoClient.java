package it.mario.trenical.server.infrastructure.adapters;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import it.mario.trenical.proto.Station;
import it.mario.trenical.proto.TrainStatus;
import it.mario.trenical.proto.TrainStatusUpdate;

import it.mario.trenical.viaggiatreno.proto.GetTrainStatusRequest;
import it.mario.trenical.viaggiatreno.proto.GetTrainStatusResponse;
import it.mario.trenical.viaggiatreno.proto.SearchStationRequest;
import it.mario.trenical.viaggiatreno.proto.SearchStationResponse;
import it.mario.trenical.viaggiatreno.proto.ViaggiaTrenoServiceGrpc;
import it.mario.trenical.viaggiatreno.proto.ViaggiaTrenoStation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PythonViaggiaTrenoClient implements ViaggiaTrenoClient {

    private static final Logger log = Logger.getLogger(PythonViaggiaTrenoClient.class.getName());

    private final ManagedChannel channel;
    private final ViaggiaTrenoServiceGrpc.ViaggiaTrenoServiceBlockingStub blocking;

    public PythonViaggiaTrenoClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blocking = ViaggiaTrenoServiceGrpc.newBlockingStub(channel);
        log.info(() -> "PythonViaggiaTrenoClient connected to " + host + ":" + port);
    }

    public PythonViaggiaTrenoClient() {
        this("localhost", 50052);
    }

    @Override
    public List<Station> searchStations(String query) {
        if (query == null || query.isBlank()) return List.of();

        try {
            SearchStationResponse resp = blocking.searchStation(
                    SearchStationRequest.newBuilder()
                            .setQuery(query)
                            .build()
            );

            List<Station> out = new ArrayList<>();
            for (ViaggiaTrenoStation s : resp.getStationsList()) {
                Station st = Station.newBuilder()
                        .setId(s.getCode())  // mapping minimale: id = code provider
                        .setName(s.getName())
                        .setCode(s.getCode())
                        .setCity(s.getCity())
                        .build();
                out.add(st);
            }
            return out;

        } catch (StatusRuntimeException e) {
            log.log(Level.WARNING, "searchStations RPC failed: " + e.getStatus(), e);
            return List.of();
        }
    }

    @Override
    public Optional<TrainStatusUpdate> getTrainStatus(String trainCode) {
        if (trainCode == null || trainCode.isBlank()) return Optional.empty();

        try {
            GetTrainStatusResponse resp = blocking.getTrainStatus(
                    GetTrainStatusRequest.newBuilder()
                            .setTrainCode(trainCode)
                            .build()
            );

            // enum esterno -> enum interno
            it.mario.trenical.viaggiatreno.proto.TrainStatus ext = resp.getStatus();
            TrainStatus status = switch (ext) {
                case TRAIN_STATUS_ON_TIME   -> TrainStatus.TRAIN_STATUS_ON_TIME;
                case TRAIN_STATUS_DELAYED   -> TrainStatus.TRAIN_STATUS_DELAYED;
                case TRAIN_STATUS_CANCELLED -> TrainStatus.TRAIN_STATUS_CANCELLED;
                case TRAIN_STATUS_UNKNOWN,
                     UNRECOGNIZED           -> TrainStatus.TRAIN_STATUS_UNKNOWN;
            };

            Timestamp ts = resp.hasLastUpdate() ? resp.getLastUpdate() : zeroTimestamp();

            TrainStatusUpdate update = TrainStatusUpdate.newBuilder()
                    .setTrainCode(resp.getTrainCode())
                    .setDelayMinutes(resp.getDelayMinutes())
                    .setPlatform(resp.getPlatform())
                    .setStatus(status)
                    .setEventTime(ts)
                    .build();

            return Optional.of(update);

        } catch (StatusRuntimeException e) {
            log.log(Level.WARNING, "getTrainStatus RPC failed: " + e.getStatus(), e);
            return Optional.of(
                    TrainStatusUpdate.newBuilder()
                            .setTrainCode(trainCode)
                            .setDelayMinutes(0)
                            .setPlatform("")
                            .setStatus(TrainStatus.TRAIN_STATUS_UNKNOWN)
                            .setEventTime(zeroTimestamp())
                            .build()
            );
        }
    }

    private static Timestamp zeroTimestamp() {
        return Timestamp.newBuilder().setSeconds(0).setNanos(0).build();
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) { }
    }
}

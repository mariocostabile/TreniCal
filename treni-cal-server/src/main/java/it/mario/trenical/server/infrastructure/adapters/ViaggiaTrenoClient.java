package it.mario.trenical.server.infrastructure.adapters;

import it.mario.trenical.proto.Station;
import it.mario.trenical.proto.TrainStatusUpdate;

import java.util.List;
import java.util.Optional;

public interface ViaggiaTrenoClient {
    List<Station> searchStations(String query);
    Optional<TrainStatusUpdate> getTrainStatus(String trainCode);
}

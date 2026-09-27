package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Signal and station state, refreshed about once per second. */
public class StatusData {
    /** Signal blocks with a train in them. */
    public Set<UUID> occupiedGroups = new HashSet<>();
    /** Signal blocks reserved for an approaching train. */
    public Set<UUID> reservedGroups = new HashSet<>();
    /** Per signal id: RED/YELLOW/GREEN/INVALID for each side, in the order of {@link SignalData#sides}. */
    public Map<UUID, List<String>> signals = new HashMap<>();
    public Map<UUID, StationStatus> stations = new HashMap<>();

    public static class StationStatus {
        @Nullable
        public UUID presentTrain;
        @Nullable
        public UUID imminentTrain;
        /** Next trains heading here, soonest first (the same data Create's display boards use). */
        public List<Departure> departures = new ArrayList<>();

        public StationStatus(GlobalStation station, int maxDepartures) {
            Train present = station.getPresentTrain();
            Train imminent = station.getImminentTrain();
            presentTrain = present == null ? null : present.id;
            imminentTrain = imminent == null ? null : imminent.id;
            for (GlobalTrainDisplayData.TrainDeparturePrediction prediction : GlobalTrainDisplayData.prepare(station.name, maxDepartures)) {
                departures.add(new Departure(prediction));
            }
        }
    }

    public static class Departure {
        public UUID trainId;
        public String trainName;
        /** Ticks until arrival (/20 for seconds). */
        public int ticks;
        /** Where the train goes after this station. */
        public String destination;
        @Nullable
        public String scheduleTitle;

        Departure(GlobalTrainDisplayData.TrainDeparturePrediction prediction) {
            trainId = prediction.train.id;
            trainName = prediction.train.name.getString();
            ticks = prediction.ticks;
            destination = prediction.destination;
            scheduleTitle = prediction.scheduleTitle == null ? null : prediction.scheduleTitle.getString();
        }
    }
}

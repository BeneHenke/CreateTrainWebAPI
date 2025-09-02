package eu.cronmoth.createtrainwebapi;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import eu.cronmoth.createtrainwebapi.model.TrainData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackInformation {
    public static List<TrainData> GetTrainData() {
        GlobalRailwayManager railway = Create.RAILWAYS;
        Map<UUID, Train> trains = railway.trains;
        List<TrainData> data = new ArrayList<>();
        for (UUID uuid : trains.keySet()) {
            Train train = trains.get(uuid);
            data.add(new TrainData(train));
        }
        return data;
    }

    public static List<TrainData> GetTrackData() {
        GlobalRailwayManager railway = Create.RAILWAYS;
        Map<UUID, Train> trains = railway.trains;
        List<TrainData> data = new ArrayList<>();
        for (UUID uuid : trains.keySet()) {
            Train train = trains.get(uuid);
            data.add(new TrainData(train));
        }
        return data;
    }
}

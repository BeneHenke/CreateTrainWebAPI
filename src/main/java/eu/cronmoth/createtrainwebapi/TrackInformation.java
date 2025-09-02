package eu.cronmoth.createtrainwebapi;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackGraph;
import eu.cronmoth.createtrainwebapi.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackInformation {
    public static GlobalRailwayManager railway = Create.RAILWAYS;

    public static List<TrainData> GetTrainData() {
        Map<UUID, Train> trains = railway.trains;
        List<TrainData> data = new ArrayList<>();
        for (UUID uuid : trains.keySet()) {
            Train train = trains.get(uuid);
            data.add(new TrainData(train));
        }
        return data;
    }

    public static NetworkData GetNetworkData() {
        Map<UUID, TrackGraph> graphs = railway.trackNetworks;
        for (UUID uuid : graphs.keySet()) {
            TrackGraph trackGraph = graphs.get(uuid);
            trackGraph.getNodes();
        }
        List<EdgeData> tracks = new ArrayList<>();
        List<PortalData> portals = new ArrayList<>();
        List<StationData> stations = new ArrayList<>();
        // TODO: Traverse track graphs and populate above lists
        return new NetworkData();
    }

    public static List<SignalData> GetSignalData() {
        // TODO: Extract signals, their sides, states, blocks
        return new ArrayList<>();
    }

    public static List<BlockData> GetBlockData() {
        // TODO: Extract blocks, occupancy, reservation, segments
        return new ArrayList<>();
    }
}

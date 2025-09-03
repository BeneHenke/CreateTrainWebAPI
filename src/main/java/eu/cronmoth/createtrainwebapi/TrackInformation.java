package eu.cronmoth.createtrainwebapi;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import eu.cronmoth.createtrainwebapi.model.*;

import java.util.*;

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

    public static List<NetworkData> GetNetworkData() {
        Map<UUID, TrackGraph> graphs = railway.trackNetworks;
        List<NetworkData> data = new ArrayList<>();
        for (UUID uuid : graphs.keySet()) {
            Set<NodeData> nodes = new HashSet<>();
            Set<EdgeData> edges = new HashSet<>();
            TrackGraph trackGraph = graphs.get(uuid);
            Set<TrackNodeLocation> trackNodes = trackGraph.getNodes();
            for (TrackNodeLocation trackNodeLocation : trackNodes) {
                TrackNode node = trackGraph.locateNode(trackNodeLocation);
                nodes.add(new NodeData(node));
                Map<TrackNode, TrackEdge> nodeEdgeMap = trackGraph.getConnectionsFrom(node);
                for (TrackEdge trackEdge : nodeEdgeMap.values()) {
                    edges.add(new EdgeData(trackEdge));
                }
            }
            data.add(new NetworkData(nodes, edges));
        }
        return data;
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

package eu.cronmoth.createtrainwebapi;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.SignalBlock;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;
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
        // For each graph, extract nodes and edges
        for (UUID uuid : graphs.keySet()) {
            Set<NodeData> nodes = new HashSet<>();
            Set<EdgeData> edges = new HashSet<>();
            TrackGraph trackGraph = graphs.get(uuid);
            Set<TrackNodeLocation> trackNodes = trackGraph.getNodes();
            Set<EdgeWrapper> trackEdges = new HashSet<>();
            // For each node, extract its data and connected edges
            for (TrackNodeLocation trackNodeLocation : trackNodes) {
                TrackNode node = trackGraph.locateNode(trackNodeLocation);
                nodes.add(new NodeData(node));
                Map<TrackNode, TrackEdge> nodeEdgeMap = trackGraph.getConnectionsFrom(node);
                // Find all edges
                for (TrackEdge trackEdge : nodeEdgeMap.values()) {
                    trackEdges.add(new EdgeWrapper(trackEdge));
                }
            }
            for (EdgeWrapper edgeWrapper : trackEdges) {
                TrackEdge trackEdge = edgeWrapper.trackEdge;
                List<TrackEdgePoint> edgePoints = trackEdge.getEdgeData().getPoints();
                boolean forward = true;
                boolean backward = true;
                // Determine directionality based on edge points
                for (TrackEdgePoint trackEdgePoint : edgePoints) {
                    if (trackEdgePoint instanceof GlobalStation) {
                        GlobalStation station = (GlobalStation) trackEdgePoint;
                        TrackNode primary = trackGraph.locateNode(station.edgeLocation.getSecond());
                    }
                    else if (trackEdgePoint instanceof SignalBoundary) {
                        //Block Entity Maps hold enttities for each direction. CanNavigate checks if both direction are set.
                        SignalBoundary signalBoundary = (SignalBoundary) trackEdgePoint;
                        forward = signalBoundary.canNavigateVia(trackEdge.node1);
                        backward = signalBoundary.canNavigateVia(trackEdge.node2);
                    }
                }
                edges.add(new EdgeData(trackEdge, forward, backward));
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

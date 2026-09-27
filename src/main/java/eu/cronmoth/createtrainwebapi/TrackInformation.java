package eu.cronmoth.createtrainwebapi;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import eu.cronmoth.createtrainwebapi.model.*;
import net.minecraft.server.MinecraftServer;

import java.util.*;

/** Reads Create's railway data. Must run on the server thread (see {@link LiveSnapshots}). */
public class TrackInformation {
    public static GlobalRailwayManager railway = Create.RAILWAYS;
    private static final int DEPARTURES_PER_STATION = 5;

    public static List<TrainData> GetTrainData(MinecraftServer server, DataOptions options) {
        List<TrainData> data = new ArrayList<>();
        for (Train train : railway.trains.values()) {
            data.add(new TrainData(train, server, options));
        }
        return data;
    }

    public static NetworkData GetNetworkData(DataOptions options) {
        Set<NodeData> nodes = new HashSet<>();
        Set<EdgeData> edges = new HashSet<>();
        Set<StationData> stations = new HashSet<>();
        // optional groups stay null when disabled
        List<SignalData> signals = options.signals() ? new ArrayList<>() : null;
        List<NetworkInfoData> networks = new ArrayList<>();
        List<PortalData> portals = options.portals() ? new ArrayList<>() : null;
        for (TrackGraph trackGraph : railway.trackNetworks.values()) {
            networks.add(new NetworkInfoData(trackGraph));
            Set<EdgeWrapper> trackEdges = new HashSet<>();
            // For each node, extract its data and connected edges
            for (TrackNodeLocation trackNodeLocation : trackGraph.getNodes()) {
                TrackNode node = trackGraph.locateNode(trackNodeLocation);
                if (node == null)
                    continue;
                NodeData nodeData = new NodeData(node, trackGraph.id);
                nodes.add(nodeData);
                for (TrackEdge trackEdge : trackGraph.getConnectionsFrom(node).values()) {
                    trackEdges.add(new EdgeWrapper(trackEdge));
                    if (trackEdge.isInterDimensional()) {
                        nodeData.interDimensional = true;
                    }
                }
            }
            for (EdgeWrapper edgeWrapper : trackEdges) {
                TrackEdge trackEdge = edgeWrapper.trackEdge;
                boolean forward = true;
                boolean backward = true;
                List<Double> signalPositions = new ArrayList<>();
                for (TrackEdgePoint trackEdgePoint : trackEdge.getEdgeData().getPoints()) {
                    if (trackEdgePoint instanceof GlobalStation station) {
                        stations.add(new StationData(station, trackGraph));
                    } else if (trackEdgePoint instanceof SignalBoundary signalBoundary) {
                        //Block Entity Maps hold enttities for each direction. CanNavigate checks if both direction are set.
                        if (signalBoundary.blockEntities.either(Map::isEmpty)) {
                            forward = !signalBoundary.canNavigateVia(trackEdge.node1);
                            backward = !signalBoundary.canNavigateVia(trackEdge.node2);
                        }
                        if (signals != null) {
                            signals.add(new SignalData(signalBoundary, trackEdge));
                            signalPositions.add(signalBoundary.getLocationOn(trackEdge));
                        }
                    }
                }
                edges.add(new EdgeData(trackEdge, forward, backward, trackGraph,
                        signals != null ? signalSegments(trackEdge, trackGraph, signalPositions) : null));
                if (portals != null && trackEdge.isInterDimensional()) {
                    portals.add(new PortalData(trackEdge));
                }
            }
        }
        return new NetworkData(nodes, edges, stations, signals, networks, portals);
    }

    /** Splits an edge at its signals; each part belongs to the signal block Create reports for its middle. */
    private static List<SignalSegmentData> signalSegments(TrackEdge edge, TrackGraph graph, List<Double> signalPositions) {
        List<SignalSegmentData> segments = new ArrayList<>();
        if (edge.isInterDimensional())
            return segments;
        double length = edge.getLength();
        List<Double> bounds = new ArrayList<>();
        bounds.add(0.0);
        signalPositions.stream().sorted().forEach(bounds::add);
        bounds.add(length);
        for (int i = 0; i + 1 < bounds.size(); i++) {
            double start = bounds.get(i);
            double end = bounds.get(i + 1);
            if (end - start < 1e-6)
                continue;
            UUID group = edge.getEdgeData().getGroupAtPosition(graph, (start + end) / 2);
            segments.add(new SignalSegmentData(start, end, group));
        }
        return segments;
    }

    public static StatusData GetStatusData(DataOptions options) {
        StatusData status = new StatusData(options);
        if (options.signals()) {
            for (SignalEdgeGroup group : railway.signalEdgeGroups.values()) {
                if (!group.trains.isEmpty()) {
                    status.occupiedGroups.add(group.id);
                } else if (group.reserved != null) {
                    status.reservedGroups.add(group.id);
                }
            }
        }
        // arrival predictions are refreshed by Create itself every 100 ticks (5 s)
        for (TrackGraph trackGraph : railway.trackNetworks.values()) {
            if (options.signals()) {
                for (SignalBoundary signal : trackGraph.getPoints(EdgePointType.SIGNAL)) {
                    status.signals.put(signal.id, List.of(signal.cachedStates.getFirst().name(), signal.cachedStates.getSecond().name()));
                }
            }
            if (options.stationArrivals()) {
                for (GlobalStation station : trackGraph.getPoints(EdgePointType.STATION)) {
                    status.stations.put(station.id, new StatusData.StationStatus(station, DEPARTURES_PER_STATION));
                }
            }
        }
        return status;
    }
}

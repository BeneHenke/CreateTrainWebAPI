package eu.cronmoth.createtrainwebapi.model;

import java.util.UUID;

import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.station.GlobalStation;

public class StationData {
    public UUID id;
    public String name;
    public boolean assembling;
    public double positionOnTrack;
    public NodeData node1;
    public NodeData node2;
    public UUID graphId;

    public StationData(GlobalStation station, TrackGraph graph) {
        id = station.id;
        name = station.name;
        assembling = station.assembling;
        positionOnTrack = station.position;
        node1 = new NodeData(graph.locateNode(station.edgeLocation.getFirst()), graph.id);
        node2 = new NodeData(graph.locateNode(station.edgeLocation.getSecond()), graph.id);
        graphId = graph.id;
    }
}

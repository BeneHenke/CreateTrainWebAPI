package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.station.GlobalStation;

import java.util.UUID;

public class StationData {
    public UUID id;
    public String name;
    public boolean assembling;
    public double positionOnTrack;
    public NodeData node1;
    public NodeData node2;
    public StationData(GlobalStation station, TrackGraph graph) {
        id = station.id;
        name = station.name.toString();
        assembling = station.assembling;
        positionOnTrack = station.position;
        node1 = new NodeData(graph.locateNode(station.edgeLocation.getFirst()));
        node2 = new NodeData(graph.locateNode(station.edgeLocation.getSecond()));
    }
}
package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackEdge;

public class EdgeData {
    public int node1;
    public int node2;

    public EdgeData(TrackEdge trackEdge) {
        node1 = trackEdge.node1.getNetId();
        node2 = trackEdge.node2.getNetId();
        
    }
}

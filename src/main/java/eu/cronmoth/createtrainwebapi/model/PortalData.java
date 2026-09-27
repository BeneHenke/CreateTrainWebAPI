package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackEdge;

/** Two track nodes in different dimensions, linked through a portal. */
public class PortalData {
    public int node1;
    public int node2;
    public DimensionLocationData location1;
    public DimensionLocationData location2;

    public PortalData(TrackEdge edge) {
        node1 = edge.node1.getNetId();
        node2 = edge.node2.getNetId();
        location1 = new DimensionLocationData(edge.node1.getLocation());
        location2 = new DimensionLocationData(edge.node2.getLocation());
    }
}

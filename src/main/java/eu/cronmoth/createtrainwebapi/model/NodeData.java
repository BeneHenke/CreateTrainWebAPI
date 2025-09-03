package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackNode;

public class NodeData {
    public DimensionLocationData dimensionLocationData;
    public int id;

    public NodeData(TrackNode node) {
        id = node.getNetId();
        dimensionLocationData = new DimensionLocationData(node.getLocation());
    }
}

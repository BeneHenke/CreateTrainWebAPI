package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackNode;

import java.util.UUID;

public class NodeData {
    public DimensionLocationData dimensionLocationData;
    public int id;
    public boolean interDimensional;
    public UUID graphId;

    public NodeData(TrackNode node, UUID graphId) {
        id = node.getNetId();
        dimensionLocationData = new DimensionLocationData(node.getLocation());
        this.graphId = graphId;
    }
}

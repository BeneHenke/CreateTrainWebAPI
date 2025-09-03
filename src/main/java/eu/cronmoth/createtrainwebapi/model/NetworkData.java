package eu.cronmoth.createtrainwebapi.model;

import java.util.Set;

public class NetworkData {
    public Set<NodeData> nodes;
    public Set<EdgeData> edges;

    public NetworkData(Set<NodeData> nodes, Set<EdgeData> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }
}

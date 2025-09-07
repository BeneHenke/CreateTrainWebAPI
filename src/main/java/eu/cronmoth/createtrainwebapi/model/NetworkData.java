package eu.cronmoth.createtrainwebapi.model;

import java.util.Set;

public class NetworkData {
    public Set<NodeData> nodes;
    public Set<EdgeData> edges;
    public Set<StationData > stations;

    public NetworkData(Set<NodeData> nodes, Set<EdgeData> edges, Set<StationData> stations) {
        this.stations = stations;
        this.nodes = nodes;
        this.edges = edges;
    }
}

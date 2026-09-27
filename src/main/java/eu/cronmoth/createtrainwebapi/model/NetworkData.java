package eu.cronmoth.createtrainwebapi.model;

import java.util.List;
import java.util.Set;

public class NetworkData {
    public Set<NodeData> nodes;
    public Set<EdgeData> edges;
    public Set<StationData> stations;
    public List<SignalData> signals;
    /** Separate track networks, with the colour Create assigns them. */
    public List<NetworkInfoData> networks;
    /** Pairs of nodes linked through a portal. */
    public List<PortalData> portals;

    public NetworkData(Set<NodeData> nodes, Set<EdgeData> edges, Set<StationData> stations, List<SignalData> signals,
                       List<NetworkInfoData> networks, List<PortalData> portals) {
        this.stations = stations;
        this.nodes = nodes;
        this.edges = edges;
        this.signals = signals;
        this.networks = networks;
        this.portals = portals;
    }
}

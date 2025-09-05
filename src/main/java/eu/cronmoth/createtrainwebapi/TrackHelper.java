package eu.cronmoth.createtrainwebapi;

import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;

import java.util.HashSet;
import java.util.Set;

public class TrackHelper {
    public static Set<TrackNode> GetNodes(TrackGraph trackGraph) {
        Set<TrackNodeLocation> trackNodes = trackGraph.getNodes();
        Set<TrackNode> nodes = new HashSet<>();
        for (TrackNodeLocation trackNodeLocation : trackNodes) {
            TrackNode node = trackGraph.locateNode(trackNodeLocation);
            nodes.add(node);
        }
        return nodes;
    }
}

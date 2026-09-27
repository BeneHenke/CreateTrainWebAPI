package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SignalBoundary;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A signal boundary on the track. The live state of each side comes from {@link StatusData}. */
public class SignalData {
    public UUID id;
    public int node1;
    public int node2;
    /** Distance from node1 along the edge. */
    public double positionOnTrack;
    public List<Side> sides = new ArrayList<>();

    /**
     * One direction of the boundary. {@code towardsNode} is the edge node on this side, so the side's
     * light can be drawn slightly towards it. Only sides with a signal block ({@code present}) show a light.
     */
    public static class Side {
        public int towardsNode;
        public boolean present;
        /** ENTRY_SIGNAL or CROSS_SIGNAL. */
        public String type;

        Side(int towardsNode, boolean present, String type) {
            this.towardsNode = towardsNode;
            this.present = present;
            this.type = type;
        }
    }

    public SignalData(SignalBoundary signal, TrackEdge edge) {
        id = signal.id;
        node1 = edge.node1.getNetId();
        node2 = edge.node2.getNetId();
        positionOnTrack = signal.getLocationOn(edge);
        for (boolean first : new boolean[]{true, false}) {
            TrackNode node = signal.isPrimary(edge.node1) == first ? edge.node1 : edge.node2;
            sides.add(new Side(node.getNetId(), !signal.blockEntities.get(first).isEmpty(), signal.types.get(first).name()));
        }
    }
}

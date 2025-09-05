package eu.cronmoth.createtrainwebapi;

import com.simibubi.create.content.trains.graph.TrackEdge;

import java.util.Objects;

public class EdgeWrapper {
    public TrackEdge trackEdge;

    public EdgeWrapper(TrackEdge trackEdge) {
        this.trackEdge = trackEdge;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EdgeWrapper)) return false;

        EdgeWrapper other = (EdgeWrapper) obj;

        long id1a = this.trackEdge.node1.getNetId();
        long id2a = this.trackEdge.node2.getNetId();
        long id1b = other.trackEdge.node1.getNetId();
        long id2b = other.trackEdge.node2.getNetId();

        // Normalize order (smaller ID first)
        long minA = Math.min(id1a, id2a);
        long maxA = Math.max(id1a, id2a);
        long minB = Math.min(id1b, id2b);
        long maxB = Math.max(id1b, id2b);

        return minA == minB && maxA == maxB;
    }

    @Override
    public int hashCode() {
        long id1 = trackEdge.node1.getNetId();
        long id2 = trackEdge.node2.getNetId();
        long min = Math.min(id1, id2);
        long max = Math.max(id1, id2);

        return Objects.hash(min, max);
    }
}

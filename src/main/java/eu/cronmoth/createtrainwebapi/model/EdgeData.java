package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.track.BezierConnection;

public class EdgeData {
    public int node1;
    public int node2;
    public boolean forwards;
    public boolean backwards;
    public BezierCurve bezierConnection;

    public EdgeData(TrackEdge trackEdge, boolean forwards, boolean backwards) {
        node1 = trackEdge.node1.getNetId();
        node2 = trackEdge.node2.getNetId();
        this.forwards = forwards;
        this.backwards = backwards;
        if (trackEdge.getTurn()!=null) {
            bezierConnection = new BezierCurve(trackEdge.getTurn());
        }
    }
}

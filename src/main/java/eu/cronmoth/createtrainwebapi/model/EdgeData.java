package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;

import java.util.List;
import java.util.UUID;

public class EdgeData {
    public int node1;
    public int node2;
    public boolean forwards;
    public boolean backwards;
    public BezierCurveData bezierConnection;
    public UUID graphId;
    /** Track length in blocks. */
    public double length;
    /** Track material, e.g. "create:andesite". */
    public String material;
    /** Connects two dimensions through a portal. */
    public boolean interDimensional;
    /** Parts of the edge between signals, each belonging to one signal block. Empty for portal edges. */
    public List<SignalSegmentData> signalSegments;

    public EdgeData(TrackEdge trackEdge, boolean forwards, boolean backwards, TrackGraph graph, List<SignalSegmentData> signalSegments) {
        node1 = trackEdge.node1.getNetId();
        node2 = trackEdge.node2.getNetId();
        this.forwards = forwards;
        this.backwards = backwards;
        if (trackEdge.getTurn() != null) {
            bezierConnection = new BezierCurveData(trackEdge.getTurn());
        }
        graphId = graph.id;
        interDimensional = trackEdge.isInterDimensional();
        length = interDimensional ? 0 : trackEdge.getLength();
        material = trackEdge.getTrackMaterial().id.toString();
        this.signalSegments = signalSegments;
    }
}

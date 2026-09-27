package eu.cronmoth.createtrainwebapi.model;

import java.util.UUID;

/** Part of an edge (distance along the edge from node1) that belongs to one signal block. */
public class SignalSegmentData {
    public double start;
    public double end;
    public UUID group;

    public SignalSegmentData(double start, double end, UUID group) {
        this.start = start;
        this.end = end;
        this.group = group;
    }
}

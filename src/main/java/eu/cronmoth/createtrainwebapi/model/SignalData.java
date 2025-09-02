package eu.cronmoth.createtrainwebapi.model;

import java.util.UUID;

public class SignalData {
    public UUID id;
    public String dimension;
    public PointData location;
    public SignalSideData forward;
    public SignalSideData reverse;
    public SignalData(UUID id, String dimension, PointData location, SignalSideData forward, SignalSideData reverse) {
        this.id = id; this.dimension = dimension; this.location = location;
        this.forward = forward; this.reverse = reverse;
    }
}

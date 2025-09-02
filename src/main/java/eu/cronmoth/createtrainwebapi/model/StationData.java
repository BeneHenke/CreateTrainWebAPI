package eu.cronmoth.createtrainwebapi.model;

import java.util.UUID;

public class StationData {
    public UUID id;
    public String name;
    public String dimension;
    public PointData location;
    public double angle;
    public boolean assembling;
    public StationData(UUID id, String name, String dimension, PointData location, double angle, boolean assembling) {
        this.id = id; this.name = name; this.dimension = dimension; this.location = location;
        this.angle = angle; this.assembling = assembling;
    }
}
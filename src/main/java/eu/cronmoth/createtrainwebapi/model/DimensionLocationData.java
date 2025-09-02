package eu.cronmoth.createtrainwebapi.model;

public class DimensionLocationData {
    public String dimension;
    public PointData location;
    public DimensionLocationData(String dimension, PointData location) {
        this.dimension = dimension; this.location = location;
    }
}

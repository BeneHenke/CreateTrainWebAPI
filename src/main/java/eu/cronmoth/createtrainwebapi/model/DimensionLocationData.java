package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;

public class DimensionLocationData {
    public String dimension;
    public PointData location;

    public DimensionLocationData(String dimension, PointData location) {
        this.dimension = dimension;
        this.location = location;
    }

    public DimensionLocationData(TrackNodeLocation location) {
        dimension = location.dimension.toString();
        this.location = new PointData(location.getLocation());
    }
}

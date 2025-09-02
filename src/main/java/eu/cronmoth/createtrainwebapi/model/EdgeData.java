package eu.cronmoth.createtrainwebapi.model;

import java.util.List;

public class EdgeData {
    public String dimension;
    public List<PointData> path;
    public EdgeData(String dimension, List<PointData> path) {
        this.dimension = dimension; this.path = path;
    }
}

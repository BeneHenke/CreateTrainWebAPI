package eu.cronmoth.createtrainwebapi.model;

public class PortalData {
    public DimensionLocationData from;
    public DimensionLocationData to;
    public PortalData(DimensionLocationData from, DimensionLocationData to) {
        this.from = from; this.to = to;
    }
}

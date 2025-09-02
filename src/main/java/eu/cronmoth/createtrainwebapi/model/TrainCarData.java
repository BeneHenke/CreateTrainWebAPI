package eu.cronmoth.createtrainwebapi.model;

public class TrainCarData {
    public int id;
    public DimensionLocationData leading;
    public DimensionLocationData trailing;
    public PortalData portal;
    public TrainCarData(int id, DimensionLocationData leading, DimensionLocationData trailing, PortalData portalData) {
        this.id = id; this.leading = leading; this.trailing = trailing; this.portal = portalData;
    }
}

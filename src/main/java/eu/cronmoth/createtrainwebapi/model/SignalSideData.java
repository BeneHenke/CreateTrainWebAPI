package eu.cronmoth.createtrainwebapi.model;

import java.util.UUID;

public class SignalSideData {
    public String type;
    public String state;
    public double angle;
    public UUID block;
    public SignalSideData(String type, String state, double angle, UUID block) {
        this.type = type; this.state = state; this.angle = angle; this.block = block;
    }
}

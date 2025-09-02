package eu.cronmoth.createtrainwebapi.model;

import java.util.List;
import java.util.UUID;

public class BlockData {
    public UUID id;
    public boolean occupied;
    public boolean reserved;
    public List<EdgeData> segments;
    public BlockData(UUID id, boolean occupied, boolean reserved, List<EdgeData> segments) {
        this.id = id;
        this.occupied = occupied;
        this.reserved = reserved;
        this.segments = segments;
    }
}

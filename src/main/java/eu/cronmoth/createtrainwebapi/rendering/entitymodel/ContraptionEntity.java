package eu.cronmoth.createtrainwebapi.rendering.entitymodel;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluenbt.NBTName;
import lombok.Data;

import java.util.UUID;

@Data
public class ContraptionEntity implements Entity {
    Key id;
    private @NBTName("UUID") UUID uuid;
    private @NBTName("CustomName") String customName;
    private @NBTName("CustomNameVisible") boolean customNameVisible;
    private @NBTName("Pos") Vector3d pos;
    private @NBTName("Motion") Vector3d motion;
    private @NBTName("Rotation") Vector2f rotation;
    private @NBTName("Contraption") ContraptionAttribute contraption;
}

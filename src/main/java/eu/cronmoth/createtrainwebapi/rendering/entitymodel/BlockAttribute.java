package eu.cronmoth.createtrainwebapi.rendering.entitymodel;

import de.bluecolored.bluenbt.NBTName;
import lombok.Data;

@Data
public class BlockAttribute {
    @NBTName("Pos") long position;
}

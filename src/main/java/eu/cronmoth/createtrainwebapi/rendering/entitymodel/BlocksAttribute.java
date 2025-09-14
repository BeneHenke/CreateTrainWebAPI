package eu.cronmoth.createtrainwebapi.rendering.entitymodel;

import de.bluecolored.bluenbt.NBTName;

import java.util.List;

public class BlocksAttribute {
    @NBTName("BlockList")
    List<BlockAttribute> blockList;
}

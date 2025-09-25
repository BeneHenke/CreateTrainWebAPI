package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.graph.TrackNode;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;

public class TrainCarData {
    public int id;
    public double positionOnTrack;
    public String assemblyDirection;
    //public double length;
    public NodeData node1;
    public NodeData node2;

    public TrainCarData(Carriage carriage) {
        id = carriage.id;
        positionOnTrack = carriage.getLeadingPoint().position;
        //length =  carriage.getLeadingPoint().position - positionOnTrack;
        node1= new NodeData(carriage.getLeadingPoint().node1);
        node2= new NodeData(carriage.getLeadingPoint().node2);

        try {
            Field f = Carriage.class.getDeclaredField("serialisedEntity");
            f.setAccessible(true);
            CompoundTag serialisedEntity = (CompoundTag) f.get(carriage);
            CompoundTag contraptionTag = serialisedEntity.getCompound("Contraption");
            assemblyDirection = contraptionTag.getString("AssemblyDirection");
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

    }
}
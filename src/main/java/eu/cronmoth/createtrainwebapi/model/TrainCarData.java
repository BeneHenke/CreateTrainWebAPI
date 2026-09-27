package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.entity.Carriage;
import eu.cronmoth.createtrainwebapi.CreateTrainWebAPIMod;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;

public class TrainCarData {
    public int id;
    public double positionOnTrack;
    public String assemblyDirection;

    public int node1;
    public int node2;

    public double trailingPositionOnTrack;
    public int node3;
    public int node4;

    private static final Field SERIALISED_ENTITY = TrainData.findField(Carriage.class, "serialisedEntity");

    public TrainCarData(Carriage carriage) {
        id = carriage.id;
        positionOnTrack = carriage.getLeadingPoint().position;
        node1 = carriage.getLeadingPoint().node1.getNetId();
        node2 = carriage.getLeadingPoint().node2.getNetId();

        trailingPositionOnTrack = carriage.getTrailingPoint().position;
        node3 = carriage.getTrailingPoint().node1.getNetId();
        node4 = carriage.getTrailingPoint().node2.getNetId();

        assemblyDirection = readAssemblyDirection(carriage);
    }

    /** One carriage without this data must not break the whole train list, so failures leave it empty. */
    private static String readAssemblyDirection(Carriage carriage) {
        if (SERIALISED_ENTITY == null)
            return null;
        try {
            CompoundTag serialisedEntity = (CompoundTag) SERIALISED_ENTITY.get(carriage);
            if (serialisedEntity == null)
                return null;
            return serialisedEntity.getCompound("Contraption").getString("AssemblyDirection");
        } catch (IllegalAccessException | ClassCastException e) {
            CreateTrainWebAPIMod.LOGGER.debug("Could not read assembly direction of carriage {}", carriage.id, e);
            return null;
        }
    }
}

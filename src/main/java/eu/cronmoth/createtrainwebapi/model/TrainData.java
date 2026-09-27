package eu.cronmoth.createtrainwebapi.model;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackNode;
import eu.cronmoth.createtrainwebapi.CreateTrainWebAPIMod;
import net.createmod.catnip.data.Couple;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Snapshot of a train. Built on the server thread, serialised later on the web server's threads. */
public class TrainData {
    public UUID id;
    @Nullable
    public UUID owner;
    @Nullable
    public String ownerName;
    public String name;
    /** Create's train icon, e.g. "create:traditional". */
    @Nullable
    public String icon;
    /** Index Create uses for the train's colour on its in-game train map. */
    public int mapColorIndex;
    @Nullable
    public UUID graphId;
    public UUID currentStation;
    public UUID targetStation;
    public List<TrainCarData> cars;
    public boolean backwards;
    public boolean stopped;
    public boolean derailed;
    /** Blocks per tick (x20 for blocks per second). */
    public double speed;
    public double targetSpeed;
    public double maxSpeed;
    public int passengers;
    public NavigationData navigation;
    @Nullable
    public ScheduleData schedule;
    /** Node ids of the planned route to the destination, as [from, to] pairs. Empty when not navigating. */
    public List<int[]> path;

    private static final Field CURRENT_PATH = findField(Navigation.class, "currentPath");

    public TrainData(Train train, MinecraftServer server) {
        id = train.id;
        owner = train.owner;
        ownerName = owner == null ? null : server.getProfileCache().get(owner).map(GameProfile::getName).orElse(null);
        name = train.name.getString();
        icon = train.icon == null ? null : train.icon.getId().toString();
        mapColorIndex = train.mapColorIndex;
        graphId = train.graph == null ? null : train.graph.id;
        currentStation = train.currentStation;
        if (train.navigation.destination != null) {
            targetStation = train.navigation.destination.id;
        }
        backwards = train.currentlyBackwards;
        stopped = train.speed == 0;
        derailed = train.derailed;
        speed = Math.abs(train.speed);
        targetSpeed = Math.abs(train.targetSpeed);
        maxSpeed = train.maxSpeed();
        passengers = train.countPlayerPassengers();
        navigation = new NavigationData(train.navigation);
        schedule = train.runtime.getSchedule() == null ? null : new ScheduleData(train, server);
        path = readPath(train);
        cars = new ArrayList<>();
        for (Carriage carriage : train.carriages) {
            cars.add(new TrainCarData(carriage));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<int[]> readPath(Train train) {
        List<int[]> result = new ArrayList<>();
        if (CURRENT_PATH == null || train.navigation.destination == null)
            return result;
        try {
            List<Couple<TrackNode>> currentPath = (List<Couple<TrackNode>>) CURRENT_PATH.get(train.navigation);
            if (currentPath == null)
                return result;
            for (Couple<TrackNode> step : currentPath) {
                result.add(new int[]{step.getFirst().getNetId(), step.getSecond().getNetId()});
            }
        } catch (IllegalAccessException | ClassCastException e) {
            CreateTrainWebAPIMod.LOGGER.debug("Could not read train path", e);
        }
        return result;
    }

    @Nullable
    static Field findField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            CreateTrainWebAPIMod.LOGGER.warn("Field {}.{} not found, related data will be missing", type.getSimpleName(), name);
            return null;
        }
    }
}

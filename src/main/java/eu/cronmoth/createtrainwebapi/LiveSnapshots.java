package eu.cronmoth.createtrainwebapi;

import com.simibubi.create.content.trains.entity.Train;
import eu.cronmoth.createtrainwebapi.model.CargoData;
import eu.cronmoth.createtrainwebapi.model.NetworkData;
import eu.cronmoth.createtrainwebapi.model.StatusData;
import eu.cronmoth.createtrainwebapi.model.TrainData;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Create's railway data is only safe to read on the server thread, so it is copied into plain
 * model objects every few ticks here. The web server threads only ever read these snapshots.
 */
public class LiveSnapshots {
    private static final int TRAINS_INTERVAL = 4;    // 200 ms, the rate of /trainsLive
    private static final int STATUS_INTERVAL = 20;   // 1 s
    private static final int NETWORK_INTERVAL = 100; // 5 s

    private volatile List<TrainData> trains = List.of();
    @Nullable
    private volatile NetworkData network;
    private volatile StatusData status = new StatusData();
    @Nullable
    private volatile MinecraftServer server;
    private int tick;
    private final boolean[] failing = new boolean[3];

    public List<TrainData> trains() {
        return trains;
    }

    @Nullable
    public NetworkData network() {
        return network;
    }

    public StatusData status() {
        return status;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        server = event.getServer();
        tick++;
        if (tick % TRAINS_INTERVAL == 0)
            update(0, "trains", () -> trains = TrackInformation.GetTrainData(event.getServer()));
        if (tick % STATUS_INTERVAL == 0)
            update(1, "status", () -> status = TrackInformation.GetStatusData());
        if (network == null || tick % NETWORK_INTERVAL == 0)
            update(2, "network", () -> network = TrackInformation.GetNetworkData());
    }

    /** A failing snapshot keeps the previous data and is logged once, instead of every few ticks. */
    private void update(int kind, String name, Runnable action) {
        try {
            action.run();
            failing[kind] = false;
        } catch (RuntimeException e) {
            if (!failing[kind])
                CreateTrainWebAPIMod.LOGGER.error("Could not update {} data", name, e);
            failing[kind] = true;
        }
    }

    /** Cargo is read on demand; blocks the calling (web) thread until the server thread has read it. */
    @Nullable
    public CargoData cargo(UUID trainId) throws Exception {
        MinecraftServer current = server;
        if (current == null)
            return null;
        CompletableFuture<CargoData> result = current.submit(() -> {
            Train train = TrackInformation.railway.trains.get(trainId);
            return train == null ? null : new CargoData(train);
        });
        return result.get(5, TimeUnit.SECONDS);
    }
}

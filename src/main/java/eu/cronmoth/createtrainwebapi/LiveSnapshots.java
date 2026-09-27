package eu.cronmoth.createtrainwebapi;

import com.simibubi.create.content.trains.entity.Train;
import eu.cronmoth.createtrainwebapi.model.CargoData;
import eu.cronmoth.createtrainwebapi.model.NetworkData;
import eu.cronmoth.createtrainwebapi.model.StatusData;
import eu.cronmoth.createtrainwebapi.model.TrainData;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Create's railway data is only safe to read on the server thread, so it is copied into plain
 * model objects every few ticks here. The web server threads only ever read these snapshots.
 */
public class LiveSnapshots {
    private static final int TRAINS_INTERVAL = 4;    // 200 ms, the rate of /trainsLive
    private static final int STATUS_INTERVAL = 20;   // 1 s
    private static final int NETWORK_INTERVAL = 100; // 5 s

    private final Supplier<DataOptions> optionsSource;
    private volatile DataOptions options = DataOptions.NONE;
    private volatile List<TrainData> trains = List.of();
    @Nullable
    private volatile NetworkData network;
    private volatile StatusData status = new StatusData(DataOptions.NONE);
    @Nullable
    private volatile MinecraftServer server;
    private int tick;
    private final boolean[] failing = new boolean[3];

    /** @param optionsSource the config's data options; read again for every snapshot, so changes apply without a restart */
    public LiveSnapshots(Supplier<DataOptions> optionsSource) {
        this.optionsSource = optionsSource;
    }

    /** The data options the current snapshots were built with (served as /features). */
    public DataOptions options() {
        return options;
    }

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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        server = event.getServer();
        tick++;
        DataOptions current = optionsSource.get();
        boolean changed = !current.equals(options);
        options = current;
        if (changed || tick % TRAINS_INTERVAL == 0)
            update(0, "trains", () -> trains = TrackInformation.GetTrainData(event.getServer(), current));
        if (changed || tick % STATUS_INTERVAL == 0)
            update(1, "status", () -> status = TrackInformation.GetStatusData(current));
        if (changed || network == null || tick % NETWORK_INTERVAL == 0)
            update(2, "network", () -> network = TrackInformation.GetNetworkData(current));
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
        if (current == null || !options.cargo())
            return null;
        CompletableFuture<CargoData> result = current.submit(() -> {
            Train train = TrackInformation.railway.trains.get(trainId);
            return train == null ? null : new CargoData(train);
        });
        return result.get(5, TimeUnit.SECONDS);
    }
}

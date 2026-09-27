package eu.cronmoth.createtrainwebapi;


import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;


// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreateTrainWebAPIMod.MODID)
public class CreateTrainWebAPIMod {
    public static final String MODID = "createtrainwebapi";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    ApiServer apiServer = new ApiServer();
    LiveSnapshots snapshots = new LiveSnapshots(Config::dataOptions);

    public CreateTrainWebAPIMod(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(snapshots);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        apiServer.stop();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        String host = Config.SERVER_HOST.get();
        int port = Config.SERVER_PORT.get();
        String trainModelPath = Config.TRAIN_MODEL_PATH.get();
        apiServer.start(host, port, trainModelPath, snapshots);
    }
}

package eu.cronmoth.createtrainwebapi;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod(CreateTrainWebAPIMod.MODID)
public class CreateTrainWebAPIMod {

    public static final String MODID = "createtrainwebapi";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final ApiServer apiServer = new ApiServer();
    private final LiveSnapshots snapshots = new LiveSnapshots(Config::dataOptions);

    public CreateTrainWebAPIMod() {
        // Register ourselves to the Forge event bus
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(snapshots);

        // Register config
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        String host = Config.SERVER_HOST.get();
        int port = Config.SERVER_PORT.get();
        String trainModelPath = Config.TRAIN_MODEL_PATH.get();

        apiServer.start(host, port, trainModelPath, snapshots);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        apiServer.stop();
    }
}

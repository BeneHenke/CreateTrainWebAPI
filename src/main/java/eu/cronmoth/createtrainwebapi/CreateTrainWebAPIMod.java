package eu.cronmoth.createtrainwebapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.nio.file.Paths;

public class CreateTrainWebAPIMod implements ModInitializer {

    public static final String ID = "createtrainwebapi";
    public static final String NAME = "Create Train Web API";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private final ApiServer apiServer = new ApiServer();
    private final LiveSnapshots snapshots = new LiveSnapshots(Config::dataOptions);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {} in Fabric environment", ID);

        // Copy Create's railway data into the API's snapshots on the server thread
        ServerTickEvents.END_SERVER_TICK.register(snapshots::onServerTick);

        // Register server start/stop handlers
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                Config.load(Paths.get("./config"));
                String host = Config.SERVER_HOST;
                int port = Config.SERVER_PORT;
                String trainModelPath = Config.TRAIN_MODEL_PATH;

                LOGGER.info("Starting API server on {}:{} (trainModelPath={})", host, port, trainModelPath);
                apiServer.start(host, port, trainModelPath, snapshots);
            } catch (Exception e) {
                LOGGER.error("Failed to start API server", e);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                LOGGER.info("Stopping API server");
                apiServer.stop();
            } catch (Exception e) {
                LOGGER.error("Failed to stop API server", e);
            }
        });
    }
}

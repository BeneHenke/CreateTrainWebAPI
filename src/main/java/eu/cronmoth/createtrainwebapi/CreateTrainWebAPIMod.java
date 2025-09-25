package eu.cronmoth.createtrainwebapi;


import de.bluecolored.bluemap.core.map.hires.entity.EntityRendererType;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.mca.entity.EntityType;
import eu.cronmoth.createtrainwebapi.rendering.entitymodel.ContraptionEntity;
import eu.cronmoth.createtrainwebapi.rendering.ContraptionEntityRenderer;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;


public class CreateTrainWebAPIMod implements Runnable {
    public static final Logger LOGGER = LogUtils.getLogger();

    ApiServer apiServer = new ApiServer();

    private void addBluemapRegistryValues() {
        EntityRendererType.REGISTRY.register(ContraptionEntityRenderer.TYPE);
        EntityType.REGISTRY.register(new EntityType.Impl(new Key("create","stationary_contraption"), ContraptionEntity.class));
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        apiServer.stop();
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) throws Exception {
        String host = Config.SERVER_HOST.get();
        int port = Config.SERVER_PORT.get();
        apiServer.start(host, port);
    }

    @Override
    public void run() {
        addBluemapRegistryValues();
        apiServer.start("0.0.0.0", 8080);
    }
}

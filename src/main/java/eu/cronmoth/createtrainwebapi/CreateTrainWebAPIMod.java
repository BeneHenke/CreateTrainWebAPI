package eu.cronmoth.createtrainwebapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.session.Session;
import io.undertow.util.Headers;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;


// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreateTrainWebAPIMod.MODID)
public class CreateTrainWebAPIMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "createtrainwebapi";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private ObjectMapper mapper = new ObjectMapper();

    private Undertow server;

    public CreateTrainWebAPIMod(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        server.stop();
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) throws Exception {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        PathHandler pathHandler = new PathHandler();
        // HTTP GET
        pathHandler.addExactPath("/trains", exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send(mapper.writeValueAsString(TrackInformation.GetTrainData()));
        });

        // WebSocket endpoint
        WebSocketProtocolHandshakeHandler wsHandler = new WebSocketProtocolHandshakeHandler(
                new WebSocketConnectionCallback() {
                    @Override
                    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                        channel.getReceiveSetter().set(new AbstractReceiveListener() {
                            @Override
                            protected void onFullTextMessage(WebSocketChannel ch, BufferedTextMessage message) {
                                String msg = message.getData();
                                WebSockets.sendText(msg, ch, null); // Use WebSockets.sendText from io.undertow.websockets.core
                            }
                        });
                        channel.resumeReceives();
                    }
                }
        );

        pathHandler.addExactPath("/ws", wsHandler);

        server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(pathHandler)
                .build();

        server.start();
        System.out.println("Server started on http://localhost:8080 and ws://localhost:8080/ws");
        GlobalRailwayManager railway = Create.RAILWAYS;
        Map <UUID, Train> trains = railway.trains;

        for (UUID uuid : trains.keySet()) {
            Train train = trains.get(uuid);
        }
    }
}

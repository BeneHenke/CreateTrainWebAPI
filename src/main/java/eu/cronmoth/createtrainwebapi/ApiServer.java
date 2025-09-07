package eu.cronmoth.createtrainwebapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public class ApiServer {
    private Undertow server;
    private ObjectMapper mapper = new ObjectMapper();

    public void start() {
        PathHandler pathHandler = new PathHandler();
        // HTTP GET
        pathHandler.addExactPath("/trains", exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Origin"), "*");
            exchange.getResponseSender().send(mapper.writeValueAsString(TrackInformation.GetTrainData()));
        });

        pathHandler.addExactPath("/network", exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Origin"), "*");
            exchange.getResponseSender().send(mapper.writeValueAsString(TrackInformation.GetNetworkData()));
        });

        pathHandler.addExactPath("/signals", exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(mapper.writeValueAsString(TrackInformation.GetSignalData()));
        });

        pathHandler.addExactPath("/blocks", exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(mapper.writeValueAsString(TrackInformation.GetBlockData()));
        });

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
                        while (channel.isOpen()) {
                            try {
                                String update = mapper.writeValueAsString(TrackInformation.GetTrainData());
                                WebSockets.sendText(update, channel, null);
                                Thread.sleep(200);
                            } catch (Exception e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                }
        );

        pathHandler.addExactPath("/ws", wsHandler);

        server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(pathHandler)
                .build();

        server.start();
    }

    public void stop() {
        server.stop();
    }
}

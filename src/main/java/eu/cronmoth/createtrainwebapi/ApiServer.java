package eu.cronmoth.createtrainwebapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.sse.ServerSentEventConnectionCallback;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class ApiServer {
    private Undertow server;
    private ObjectMapper mapper = new ObjectMapper();

    public void start(String host, int port) {
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

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

        pathHandler.addExactPath("/trainsLive",
                exchange -> {
                    exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Origin"), "*");
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream");
                    new ServerSentEventHandler(
                            (connection, lastEventId) -> {
                                ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                                    if (connection.isOpen()) {
                                        try {
                                            String update = mapper.writeValueAsString(TrackInformation.GetTrainData());
                                            connection.send(update);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, 0, 200, TimeUnit.MILLISECONDS);

                                connection.addCloseTask(conn -> future.cancel(false));
                            }
                    ).handleRequest(exchange);
                }
        );

        if (new File("bluemap/train_models/").exists()) {
            FileResourceManager resourceManager = new FileResourceManager(new File("bluemap/train_models/"), 100);
            ResourceHandler resourceHandler = new ResourceHandler(resourceManager)
                    .setDirectoryListingEnabled(false);
            HttpHandler trainModelHandler = exchange -> {
                exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Origin"), "*");
                resourceHandler.handleRequest(exchange);
            };

            pathHandler.addPrefixPath("/trainModels", trainModelHandler);
        }
        server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(pathHandler)
                .build();
        server.start();
    }
    public void stop() {
        server.stop();
    }
}

package eu.cronmoth.createtrainwebapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cronmoth.createtrainwebapi.model.CargoData;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ApiServer {
    private static final HttpString ALLOW_ORIGIN = new HttpString("Access-Control-Allow-Origin");

    private Undertow server;
    private ScheduledExecutorService scheduler;
    private final ObjectMapper mapper = new ObjectMapper();

    public void start(String host, int port, String trainModelPath, LiveSnapshots snapshots) {
        PathHandler pathHandler = new PathHandler();
        scheduler = Executors.newScheduledThreadPool(4);

        pathHandler.addExactPath("/trains", exchange -> sendJson(exchange, snapshots.trains()));
        pathHandler.addExactPath("/network", exchange -> sendJson(exchange, snapshots.network()));
        pathHandler.addExactPath("/status", exchange -> sendJson(exchange, snapshots.status()));
        pathHandler.addExactPath("/trainsLive", liveStream(snapshots::trains, 200));
        pathHandler.addExactPath("/statusLive", liveStream(snapshots::status, 1000));

        // GET /cargo?train=<uuid>: waits for the server thread, so it must not run on an IO thread
        pathHandler.addExactPath("/cargo", exchange -> exchange.dispatch(() -> {
            try {
                Deque<String> param = exchange.getQueryParameters().get("train");
                UUID trainId = param == null ? null : UUID.fromString(param.getFirst());
                CargoData cargo = trainId == null ? null : snapshots.cargo(trainId);
                if (cargo == null) {
                    sendStatus(exchange, StatusCodes.NOT_FOUND);
                    return;
                }
                sendJson(exchange, cargo);
            } catch (Exception e) {
                sendStatus(exchange, StatusCodes.BAD_REQUEST);
            }
        }));


        File trainModelsDir = new File(trainModelPath);
        if (trainModelsDir.exists()) {
            FileResourceManager resourceManager = new FileResourceManager(trainModelsDir, 100);
            ResourceHandler resourceHandler = new ResourceHandler(resourceManager)
                    .setDirectoryListingEnabled(false);
            HttpHandler trainModelHandler = exchange -> {
                exchange.getResponseHeaders().put(ALLOW_ORIGIN, "*");
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

    private void sendJson(HttpServerExchange exchange, Object data) throws IOException {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseHeaders().put(ALLOW_ORIGIN, "*");
        exchange.getResponseSender().send(mapper.writeValueAsString(data));
    }

    private static void sendStatus(HttpServerExchange exchange, int statusCode) {
        exchange.setStatusCode(statusCode);
        exchange.getResponseHeaders().put(ALLOW_ORIGIN, "*");
        exchange.endExchange();
    }

    /** Server-sent events stream that sends the current snapshot every {@code periodMillis}. */
    private HttpHandler liveStream(Supplier<?> snapshot, long periodMillis) {
        return exchange -> {
            exchange.getResponseHeaders().put(ALLOW_ORIGIN, "*");
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream");
            new ServerSentEventHandler(
                    (connection, lastEventId) -> {
                        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                            if (connection.isOpen()) {
                                try {
                                    connection.send(mapper.writeValueAsString(snapshot.get()));
                                } catch (RejectedExecutionException e) {
                                    // XNIO worker is shutting down, nothing to do
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 0, periodMillis, TimeUnit.MILLISECONDS);

                        connection.addCloseTask(conn -> future.cancel(false));
                    }
            ).handleRequest(exchange);
        };
    }


    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (server != null) {
            server.stop();
        }
    }
}

package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.graph.TrackGraph;

import java.util.UUID;

public class NetworkInfoData {
    public UUID id;
    /** Colour Create assigns to the network, as "#rrggbb". */
    public String color;

    public NetworkInfoData(TrackGraph graph) {
        id = graph.id;
        color = String.format("#%06x", graph.color.getRGB() & 0xFFFFFF);
    }
}

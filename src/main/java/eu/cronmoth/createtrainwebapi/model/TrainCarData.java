package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.graph.TrackNode;

public class TrainCarData {
    public int id;
    public double positionOnTrack;
    public double endPositionOnTrack;
    public double startPositionOnTrack;
    public NodeData node1;
    public NodeData node2;

    public TrainCarData(Carriage carriage) {
        id = carriage.id;
        startPositionOnTrack = carriage.getLeadingPoint().position;
        endPositionOnTrack = carriage.getTrailingPoint().position;
        positionOnTrack = endPositionOnTrack;
        node1= new NodeData(carriage.getTrailingPoint().node1);
        node2= new NodeData(carriage.getTrailingPoint().node2);
    }
}
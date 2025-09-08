package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.graph.TrackNode;

public class TrainCarData {
    public int id;
    public double positionOnTrack;
    //public double length;
    public NodeData node1;
    public NodeData node2;

    public TrainCarData(Carriage carriage) {
        id = carriage.id;
        positionOnTrack = carriage.getLeadingPoint().position;
        //length =  carriage.getLeadingPoint().position - positionOnTrack;
        node1= new NodeData(carriage.getLeadingPoint().node1);
        node2= new NodeData(carriage.getLeadingPoint().node2);
    }
}
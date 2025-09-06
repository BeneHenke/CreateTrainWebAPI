package eu.cronmoth.createtrainwebapi.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackGraph;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrainData {
    public UUID id;
    @Nullable
    public UUID owner;
    @JsonIgnore
    public TrackGraph graph;
    public UUID currentStation;
    public UUID targetStation;
    public String name;
    public List<TrainCarData> cars;
    public boolean backwards;
    public boolean stopped;
    public TrainData(Train train) {
        id = train.id;
        owner = train.owner;
        //graph = train.graph.;
        currentStation = train.currentStation;
        name = train.name.toString();
        if (train.navigation.destination!=null) {
            targetStation = train.navigation.destination.id;
        }
        cars = new ArrayList<>();
        for (Carriage carriage : train.carriages) {
            cars.add(new TrainCarData(carriage));
        }
    }
}

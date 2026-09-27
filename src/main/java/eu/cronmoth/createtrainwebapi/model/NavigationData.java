package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.entity.Navigation;
import javax.annotation.Nullable;

public class NavigationData {
    @Nullable
    public String destinationName;
    /** Blocks along the track, only meaningful while navigating. */
    public double distanceToDestination;
    public boolean waitingForSignal;
    public int ticksWaitingForSignal;

    public NavigationData(Navigation navigation) {
        if (navigation.destination != null) {
            destinationName = navigation.destination.name;
            distanceToDestination = navigation.distanceToDestination;
        }
        waitingForSignal = navigation.waitingForSignal != null;
        ticksWaitingForSignal = waitingForSignal ? navigation.ticksWaitingForSignal : 0;
    }
}

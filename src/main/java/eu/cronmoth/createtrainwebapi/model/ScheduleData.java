package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

import java.util.List;

public class ScheduleData {
    @Nullable
    public String title;
    public int currentEntry;
    public int entryCount;
    /** PRE_TRANSIT, IN_TRANSIT or POST_TRANSIT. */
    public String state;
    public boolean paused;
    public boolean completed;
    public boolean cyclic;
    /** True for schedules assigned by a station (auto schedule). */
    public boolean auto;
    /** What the train is waiting for at a stop, e.g. "Waiting for cargo". */
    @Nullable
    public String waitingStatus;

    public ScheduleData(Train train, MinecraftServer server) {
        ScheduleRuntime runtime = train.runtime;
        Schedule schedule = runtime.getSchedule();
        title = runtime.currentTitle;
        currentEntry = runtime.currentEntry;
        entryCount = schedule.entries.size();
        state = runtime.state == null ? null : runtime.state.name();
        paused = runtime.paused;
        completed = runtime.completed;
        cyclic = schedule.cyclic;
        auto = runtime.isAutoSchedule;
        // wait conditions (e.g. time of day) read the level the train is in
        List<ResourceKey<Level>> dimensions = train.getPresentDimensions();
        Level level = dimensions.isEmpty() ? null : server.getLevel(dimensions.get(0));
        if (runtime.state == ScheduleRuntime.State.POST_TRANSIT && level != null) {
            Component status = runtime.getWaitingStatus(level);
            waitingStatus = status == null ? null : status.getString();
        }
    }
}

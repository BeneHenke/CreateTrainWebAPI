package eu.cronmoth.createtrainwebapi;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue SERVER_PORT = BUILDER
            .comment("Webserver Port")
            .defineInRange("serverPort", 8080, 1, 65535);
    public static final ModConfigSpec.ConfigValue<String> SERVER_HOST = BUILDER
            .comment("Webserver hostname")
            .define("serverHost", "0.0.0.0");
    public static final ModConfigSpec.ConfigValue<String> TRAIN_MODEL_PATH = BUILDER
            .comment("Path of the train models")
            .define("trainModelPath", "bluemap/train_models/");

    // Optional data; disabled groups are not collected and are left out of the answers
    static {
        BUILDER.comment("Optional information included in the API's answers.",
                "Disabled data is not collected at all, which also saves work on the server.").push("data");
    }
    private static final ModConfigSpec.BooleanValue OWNER = BUILDER
            .comment("Train owner (uuid and player name)").define("owner", true);
    private static final ModConfigSpec.BooleanValue TRAIN_STATUS = BUILDER
            .comment("Speed, max speed, derailed").define("trainStatus", true);
    private static final ModConfigSpec.BooleanValue PASSENGERS = BUILDER
            .comment("Number of players on board").define("passengers", true);
    private static final ModConfigSpec.BooleanValue NAVIGATION = BUILDER
            .comment("Destination, distance to it, waiting at a signal").define("navigation", true);
    private static final ModConfigSpec.BooleanValue SCHEDULE = BUILDER
            .comment("Schedule title, current stop, state, wait reason").define("schedule", true);
    private static final ModConfigSpec.BooleanValue ROUTE = BUILDER
            .comment("Planned route to the destination").define("route", true);
    private static final ModConfigSpec.BooleanValue STATION_ARRIVALS = BUILDER
            .comment("Per station: train at the platform, arriving train, next arrivals").define("stationArrivals", true);
    private static final ModConfigSpec.BooleanValue SIGNALS = BUILDER
            .comment("Signals, signal states and occupied signal blocks").define("signals", true);
    private static final ModConfigSpec.BooleanValue PORTALS = BUILDER
            .comment("Track nodes linked through portals").define("portals", true);
    private static final ModConfigSpec.BooleanValue CARGO = BUILDER
            .comment("Train inventories and tanks (/cargo)").define("cargo", true);
    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static DataOptions dataOptions() {
        return new DataOptions(OWNER.get(), TRAIN_STATUS.get(), PASSENGERS.get(), NAVIGATION.get(), SCHEDULE.get(),
                ROUTE.get(), STATION_ARRIVALS.get(), SIGNALS.get(), PORTALS.get(), CARGO.get());
    }

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}

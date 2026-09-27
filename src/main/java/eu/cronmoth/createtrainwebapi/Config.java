package eu.cronmoth.createtrainwebapi;

import net.minecraft.ResourceLocationException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue SERVER_PORT = BUILDER
            .comment("Webserver Port")
            .defineInRange("serverPort", 8080, 1, 65535);

    public static final ForgeConfigSpec.ConfigValue<String> SERVER_HOST = BUILDER
            .comment("Webserver hostname")
            .define("serverHost", "0.0.0.0");

    public static final ForgeConfigSpec.ConfigValue<String> TRAIN_MODEL_PATH = BUILDER
            .comment("Path of the train models")
            .define("trainModelPath", "bluemap/train_models/");

    // Optional data; disabled groups are not collected and are left out of the answers
    static {
        BUILDER.comment("Optional information included in the API's answers.",
                "Disabled data is not collected at all, which also saves work on the server.").push("data");
    }
    private static final ForgeConfigSpec.BooleanValue OWNER = BUILDER
            .comment("Train owner (uuid and player name)").define("owner", true);
    private static final ForgeConfigSpec.BooleanValue TRAIN_STATUS = BUILDER
            .comment("Speed, max speed, derailed").define("trainStatus", true);
    private static final ForgeConfigSpec.BooleanValue PASSENGERS = BUILDER
            .comment("Number of players on board").define("passengers", true);
    private static final ForgeConfigSpec.BooleanValue NAVIGATION = BUILDER
            .comment("Destination, distance to it, waiting at a signal").define("navigation", true);
    private static final ForgeConfigSpec.BooleanValue SCHEDULE = BUILDER
            .comment("Schedule title, current stop, state, wait reason").define("schedule", true);
    private static final ForgeConfigSpec.BooleanValue ROUTE = BUILDER
            .comment("Planned route to the destination").define("route", true);
    private static final ForgeConfigSpec.BooleanValue STATION_ARRIVALS = BUILDER
            .comment("Per station: train at the platform, arriving train, next arrivals").define("stationArrivals", true);
    private static final ForgeConfigSpec.BooleanValue SIGNALS = BUILDER
            .comment("Signals, signal states and occupied signal blocks").define("signals", true);
    private static final ForgeConfigSpec.BooleanValue PORTALS = BUILDER
            .comment("Track nodes linked through portals").define("portals", true);
    private static final ForgeConfigSpec.BooleanValue CARGO = BUILDER
            .comment("Train inventories and tanks (/cargo)").define("cargo", true);
    static {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static DataOptions dataOptions() {
        return new DataOptions(OWNER.get(), TRAIN_STATUS.get(), PASSENGERS.get(), NAVIGATION.get(), SCHEDULE.get(),
                ROUTE.get(), STATION_ARRIVALS.get(), SIGNALS.get(), PORTALS.get(), CARGO.get());
    }

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof String itemName)) {
            return false;
        }

        try {
            return BuiltInRegistries.ITEM.containsKey(new ResourceLocation(itemName));
        } catch (ResourceLocationException e) {
            return false;
        }
    }

}

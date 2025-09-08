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
            .define("serverHost", "localhost");

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}

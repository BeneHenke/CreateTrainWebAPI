package eu.cronmoth.createtrainwebapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

public class Config {
    public static int SERVER_PORT = 8080;
    public static String SERVER_HOST = "0.0.0.0";
    public static String TRAIN_MODEL_PATH = "bluemap/train_models/";

    private static final String FILE_NAME = "createtrainwebapi.toml";

    // Optional data ([data] section, all on by default). The file is checked for changes at most every
    // 2 s and the section re-read, so switches apply without a restart like on (Neo)Forge.
    private static final String[] DATA_KEYS = {"owner", "trainStatus", "passengers", "navigation", "schedule",
            "route", "stationArrivals", "signals", "portals", "cargo"};
    private static final long CHECK_INTERVAL_MILLIS = 2000;
    private static volatile DataOptions dataOptions = new DataOptions(true, true, true, true, true, true, true, true, true, true);
    private static Path file;
    private static long lastModified;
    private static long lastCheck;

    public static void load(Path configDir) {
        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path file = configDir.resolve(FILE_NAME);
            if (!Files.exists(file)) {
                String defaultToml = "# Create Train Web API config\n" +
                        "SERVER_PORT = 8080\n" +
                        "SERVER_HOST = \"0.0.0.0\"\n" +
                        "TRAIN_MODEL_PATH = \"bluemap/train_models/\"\n" +
                        "\n" +
                        "# Optional information included in the API's answers.\n" +
                        "# Disabled data is not collected at all, which also saves work on the server.\n" +
                        "[data]\n" +
                        "owner = true           # train owner (uuid and player name)\n" +
                        "trainStatus = true     # speed, max speed, derailed\n" +
                        "passengers = true      # number of players on board\n" +
                        "navigation = true      # destination, distance to it, waiting at a signal\n" +
                        "schedule = true        # schedule title, current stop, state, wait reason\n" +
                        "route = true           # planned route to the destination\n" +
                        "stationArrivals = true # per station: train at the platform, arriving train, next arrivals\n" +
                        "signals = true         # signals, signal states and occupied signal blocks\n" +
                        "portals = true         # track nodes linked through portals\n" +
                        "cargo = true           # train inventories and tanks (/cargo)\n";
                Files.write(file, defaultToml.getBytes(), StandardOpenOption.CREATE_NEW);
            }

            String content = Files.readString(file);
            TomlParseResult result = Toml.parse(content);

            if (result.contains("SERVER_PORT")) {
                SERVER_PORT = result.getLong("SERVER_PORT").intValue();
            }

            if (result.contains("SERVER_HOST")) {
                SERVER_HOST = result.getString("SERVER_HOST");
            }

            if (result.contains("TRAIN_MODEL_PATH")) {
                TRAIN_MODEL_PATH = result.getString("TRAIN_MODEL_PATH");
            }

            readDataOptions(result);
            Config.file = file;
            lastModified = Files.getLastModifiedTime(file).toMillis();

        } catch (IOException e) {
            // ignore and keep defaults
            e.printStackTrace();
        }
    }

    public static DataOptions dataOptions() {
        long now = System.currentTimeMillis();
        if (file != null && now - lastCheck >= CHECK_INTERVAL_MILLIS) {
            lastCheck = now;
            try {
                long modified = Files.getLastModifiedTime(file).toMillis();
                if (modified != lastModified) {
                    lastModified = modified;
                    readDataOptions(Toml.parse(Files.readString(file)));
                }
            } catch (IOException e) {
                // keep the current options
            }
        }
        return dataOptions;
    }

    /** Missing or non-boolean entries count as enabled. */
    private static void readDataOptions(TomlParseResult result) {
        boolean[] values = new boolean[DATA_KEYS.length];
        for (int i = 0; i < DATA_KEYS.length; i++) {
            String key = "data." + DATA_KEYS[i];
            values[i] = !result.isBoolean(key) || result.getBoolean(key);
        }
        dataOptions = new DataOptions(values[0], values[1], values[2], values[3], values[4],
                values[5], values[6], values[7], values[8], values[9]);
    }
}

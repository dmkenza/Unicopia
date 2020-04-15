package com.minelittlepony.unicopia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class Config {

    private static Config instance = new Config();

    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    public static Config getInstance() {
        return instance;
    }

    static void init(Path directory) {
        Path file = directory.resolve("unicopia.json");

        try {
            if (Files.exists(file)) {
                try(JsonReader reader = new JsonReader(Files.newBufferedReader(file))) {
                    instance = gson.fromJson(reader, Config.class);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (instance == null) {
                instance = new Config();
            }
        }

        instance.file = file;
        instance.save();
    }

    private Path file;

    @Expose(deserialize = false)
    private final String speciesWhiteListComment =
            "A whitelist of races permitted on the server. " +
            "Races added to this list can be used by anyone, whilst any ones left off are not permitted. " +
            "An empty list disables whitelisting entirely.";
    @Expose
    private final List<Race> speciesWhiteList = Lists.newArrayList();

    @Expose(deserialize = false)
    private final String preferredRaceComment =
            "The default preferred race. " +
            "This is the race a client requests when first joining a game. " +
            "It is the default used both when Mine Little Pony is not installed and when they respond with a human race.";
    @Expose
    private Race preferredRace = Race.EARTH;

    @Expose(deserialize = false)
    private final String ignoreMineLPComment =
            "If true Mine Little Pony will not be considered when determining the race to use. " +
            "The result will always be what is set by this config file.";
    @Expose
    private boolean ignoreMineLP = false;

    public List<Race> getSpeciesWhiteList() {
        return speciesWhiteList;
    }

    public boolean ignoresMineLittlePony() {
        return ignoreMineLP;
    }

    public void setIgnoreMineLittlePony(boolean value) {
        if (value != ignoreMineLP) {
            ignoreMineLP = value;
            save();
        }
    }

    public void setPreferredRace(Race race) {
        if (preferredRace != race) {
            preferredRace = race;
            save();
        }
    }

    public Race getPrefferedRace() {
        if (preferredRace == null) {
            setPreferredRace(Race.EARTH);
        }

        return preferredRace;
    }

    public void save() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(file))) {
            writer.setIndent("    ");

            gson.toJson(this, Config.class, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

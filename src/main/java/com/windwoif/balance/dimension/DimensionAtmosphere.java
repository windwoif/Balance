package com.windwoif.balance.dimension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.windwoif.balance.Balance;
import com.windwoif.balance.content.reactors.recipe.chemical.Chemical;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryManager;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DimensionAtmosphere {
    private static final Map<ResourceKey<Level>, Atmosphere> atmospheres = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Atmosphere DEFAULT_ATMOSPHERE;

    static {
        HashMap<Chemical, Integer> defaultMap = new HashMap<>();
//        defaultMap.put(getChemical("balance:oxygen"), 21);
//        defaultMap.put(getChemical("balance:nitrogen"), 78);
//        defaultMap.put(getChemical("balance:argon"), 1);
        DEFAULT_ATMOSPHERE = new Atmosphere(298, defaultMap);
    }

    private static Chemical getChemical(String id) {
        ResourceLocation chemicalId = ResourceLocation.parse(id);
        return RegistryManager.ACTIVE.getRegistry(Balance.CHEMICAL_REGISTRY_KEY)
                .getValue(chemicalId);
    }

    public static void init() {
        loadConfigs();

        registerDefault(Level.OVERWORLD, DEFAULT_ATMOSPHERE);
        registerDefault(Level.NETHER, createNetherAtmosphere());
        registerDefault(Level.END, createEndAtmosphere());
    }

    private static Atmosphere createNetherAtmosphere() {
        HashMap<Chemical, Integer> netherAtmosphere = new HashMap<>();
//        netherAtmosphere.put(getChemical("balance:carbon_dioxide"), 85);
//        netherAtmosphere.put(getChemical("balance:sulfur_dioxide"), 10);
//        netherAtmosphere.put(getChemical("balance:hydrogen_sulfide"), 5);
        return new Atmosphere(1200, netherAtmosphere);
    }

    private static Atmosphere createEndAtmosphere() {
        HashMap<Chemical, Integer> endAtmosphere = new HashMap<>();
//        endAtmosphere.put(getChemical("balance:helium"), 99);
//        endAtmosphere.put(getChemical("balance:neon"), 1);
        return new Atmosphere(100, endAtmosphere);
    }

    private static void registerDefault(ResourceKey<Level> dimension, Atmosphere atmosphere) {
        if (!atmospheres.containsKey(dimension)) {
            atmospheres.put(dimension, atmosphere);
        }
    }

    private static void loadConfigs() {
        Path configDir = Path.of("config", Balance.MODID, "dimensions");

        try {
            Files.createDirectories(configDir);

            createExampleConfig(configDir);

            Files.list(configDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(DimensionAtmosphere::loadConfig);

        } catch (IOException e) {
            Balance.LOGGER.error("Failed to load dimension atmosphere configs", e);
        }
    }

    private static void createExampleConfig(Path configDir) throws IOException {
        Path exampleFile = configDir.resolve("overworld_example.json");
        if (!Files.exists(exampleFile)) {
            Map<String, Object> example = new HashMap<>();
            example.put("temperature", 298);

            Map<String, Integer> atmosphere = new HashMap<>();
            atmosphere.put("balance:oxygen", 21);
            atmosphere.put("balance:nitrogen", 78);
            atmosphere.put("balance:argon", 1);

            example.put("atmosphere", atmosphere);

            try (Writer writer = Files.newBufferedWriter(exampleFile)) {
                GSON.toJson(example, writer);
            }
        }
    }

    private static void loadConfig(Path configPath) {
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> config = GSON.fromJson(reader, type);

            int temperature = ((Double) config.get("temperature")).intValue();
            Map<String, Double> atmosphereRaw = (Map<String, Double>) config.get("atmosphere");

            HashMap<Chemical, Integer> atmosphereMap = new HashMap<>();
            for (Map.Entry<String, Double> entry : atmosphereRaw.entrySet()) {
                Chemical chemical = getChemical(entry.getKey());
                if (chemical != null) {
                    atmosphereMap.put(chemical, (int)(entry.getValue() * 100));
                }
            }

            String fileName = configPath.getFileName().toString();
            String dimensionName = fileName.substring(0, fileName.length() - 5);

            ResourceKey<Level> dimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(dimensionName.replace('_', ':'))
            );

            atmospheres.put(dimension, new Atmosphere(temperature, atmosphereMap));

            Balance.LOGGER.info("Loaded atmosphere config for dimension: {}", dimension.location());

        } catch (Exception e) {
            Balance.LOGGER.error("Failed to load config: {}", configPath, e);
        }
    }

    public static Atmosphere getAtmosphere(ResourceKey<Level> dimension) {
        return atmospheres.getOrDefault(dimension, DEFAULT_ATMOSPHERE);
    }

    public static void setAtmosphere(ResourceKey<Level> dimension, Atmosphere atmosphere) {
        atmospheres.put(dimension, atmosphere);
    }

    public static Map<ResourceKey<Level>, Atmosphere> getAllAtmospheres() {
        return new HashMap<>(atmospheres);
    }
}

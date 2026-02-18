package com.windwoif.balance.content.reactors.reactorCore;

import com.mojang.logging.LogUtils;
import com.windwoif.balance.Balance;
import com.windwoif.balance.Chemical;
import com.windwoif.balance.Chemicals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.slf4j.Logger;

import java.util.*;

import static com.windwoif.balance.Balance.CHEMICAL_REGISTRY_KEY;

public class Container {
    private float temperature;//TODO
    protected float heat;
    private final long Volume;
    private final long BasicHeatCapacity;

    public Container(long volume, long temp, long heatCapacity){
        Volume = volume;
        BasicHeatCapacity = heatCapacity;
        heat = temp * heatCapacity;
        temperature = temp;
        Phase liquidPolarPhase = new Phase(Chemical.State.LIQUID_POLAR);
        Phase liquidNonpolarPhase = new Phase(Chemical.State.LIQUID_NONPOLAR);
        phases.put(Chemical.State.SOLID, new Phase(Chemical.State.SOLID));
        phases.put(Chemical.State.LIQUID_POLAR, liquidPolarPhase);
        phases.put(Chemical.State.AQUEOUS, liquidPolarPhase);
        phases.put(Chemical.State.LIQUID_NONPOLAR, liquidNonpolarPhase);
        phases.put(Chemical.State.ORGANIC, liquidNonpolarPhase);
        phases.put(Chemical.State.GAS, new Phase(Chemical.State.GAS));
        phases.put(Chemical.State.MOLTEN_SALT, new Phase(Chemical.State.MOLTEN_SALT));
        phases.put(Chemical.State.MOLTEN_METAL, new Phase(Chemical.State.MOLTEN_METAL));
        sortedPhases.add(phases.get(Chemical.State.SOLID));
        sortedPhases.add(phases.get(Chemical.State.LIQUID_POLAR));
        sortedPhases.add(phases.get(Chemical.State.LIQUID_NONPOLAR));
        sortedPhases.add(phases.get(Chemical.State.GAS));
        sortedPhases.add(phases.get(Chemical.State.MOLTEN_SALT));
        sortedPhases.add(phases.get(Chemical.State.MOLTEN_METAL));
    }
    private Runnable markChangedCallback;
    public void setMarkChangedCallback(Runnable callback) {
        this.markChangedCallback = callback;
    }
    protected void markChanged() {
        if (markChangedCallback != null) markChangedCallback.run();
    }
    private static final Logger LOGGER = LogUtils.getLogger();

    protected void tick(){
        updateTemp();
        updateVolume();
        updateDensity();
        updatePhaseOrder();
    }

    private void updateTemp() {
        float heatCapacity = calculateHeatCapacity();
        temperature = heat / heatCapacity;
    }
    private float calculateHeatCapacity(){
        return BasicHeatCapacity + contents.entrySet().stream()
                .map(a -> a.getKey().getHeatCapacity(a.getValue()))
                .reduce(0f, Float::sum);
    }

    private final Map<Chemical, Long> contents = new HashMap<>();
    private final EnumMap<Chemical.State, Phase> phases = new EnumMap<>(Chemical.State.class);
    private final List<Phase> sortedPhases = new ArrayList<>();

    public Phase getPhase(Chemical.State state) {
        return phases.get(state);
    }

    private Map<Chemical, Long> getChemicalMap(Chemical.State state) {
        return getPhase(state).getContents();
    }
    public double getContentVolume(Chemical.State state) {
        return getPhase(state).getVolume();
    }

    private void updateVolume(){
        double occupied = 0;
        for (Map.Entry<Chemical.State, Phase> entry : phases.entrySet()) {
            if (entry.getKey() != Chemical.State.GAS) {
                entry.getValue().updateVolume();
                occupied += entry.getValue().getVolume();
            }
        }
        phases.get(Chemical.State.GAS).setVolume(Volume - occupied);
    }

    private void updateDensity() {
        phases.forEach((state, phase) -> phase.updateDensity());
    }

    private void syncStateMaps() {
        phases.forEach((state, phase) -> phase.clear());
        contents.forEach(this::updateStateMaps);
    }
    public void updateStateMaps(Chemical chemical, long amount) {
        Map<Chemical, Long> targetMap = getChemicalMap(chemical.state());
        if (amount >= 0) {
            targetMap.put(chemical, amount);
        }
    }

    private void updatePhaseOrder() {
        sortedPhases.sort(Comparator.comparingDouble(Phase::getDensity).reversed());
    }


    public List<Phase> getSortedPhases() {
        return sortedPhases;
    }

    public void changeChemical(Chemical chemical, long amount) {
        contents.merge(chemical, amount, Long::sum);
        if (contents.getOrDefault(chemical, 0L) < 0) LOGGER.error("Overcost chemicals");
        updateStateMaps(chemical, contents.get(chemical));
        markChanged();
    }

    public double getConcentration(Chemical chemical) {
        double amount = GetChemical(chemical);
        return switch (chemical.state()) {
            case ORGANIC, AQUEOUS, GAS -> amount / getContentVolume(chemical.state());
            default -> 1;
        };
    }

    public Component check() {
        if (contents.isEmpty()) return Component.literal("Empty");
        MutableComponent message = Component.literal("Content:\n");
        contents.forEach((chemical, amount) ->
                message.append(Component.literal(String.format("- %s: %d mol\n", chemical.name(), amount/1000))));
        return message;
    }

    public void LoadChemicals(ListTag chemicalsList) {
        contents.clear();
        syncStateMaps();
        for (int i = 0; i < chemicalsList.size(); i++) {
            CompoundTag chemicalTag = chemicalsList.getCompound(i);
            ResourceLocation chemicalId = ResourceLocation.parse(chemicalTag.getString("id"));
            long amount = chemicalTag.getLong("amount");
            IForgeRegistry<Chemical> registry = RegistryManager.ACTIVE.getRegistry(Balance.CHEMICAL_REGISTRY_KEY);
            Chemical chemical = registry.getValue(chemicalId);
            if (chemical != null) {
                contents.put(chemical, amount);
                updateStateMaps(chemical, amount);
            }
        }
    }
    public ListTag SaveChemicals() {
        ListTag chemicalsList = new ListTag();
        contents.forEach((chemical, amount) -> {
            IForgeRegistry<Chemical> registry = RegistryManager.ACTIVE.getRegistry(CHEMICAL_REGISTRY_KEY);
            ResourceLocation chemicalId = registry.getKey(chemical);
            if (chemicalId != null) {
                CompoundTag chemicalTag = new CompoundTag();
                chemicalTag.putString("id", chemicalId.toString());
                chemicalTag.putLong("amount", amount);
                chemicalsList.add(chemicalTag);
            }
        });
        return chemicalsList;
    }
    public Component testFill() {//milk

        changeChemical(Chemicals.WATER.get(), 1000000000000000000L);
        return Component.literal("filled with NO");
    }
    public Component testFill2() {
        changeChemical(Chemicals.HYDROGEN_ION.get(), 10000L);
        changeChemical(Chemicals.HYDROXIDE.get(), 10000L);
        return Component.literal("filled with NO@");
    }
    public double GetChemical(Chemical chemical) {
        return (double) contents.getOrDefault(chemical, 0L) / 1000;
    }
    public long getVolume() {
        return Volume;
    }
    public float getTemperature() {
//        return temperature;
        return 298;
    }
}

package com.windwoif.balance.content.reactors.reactorCore;

import com.mojang.logging.LogUtils;
import com.windwoif.balance.Balance;
import com.windwoif.balance.content.reactors.recipe.chemical.Chemical;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.slf4j.Logger;

import java.util.*;

import static com.windwoif.balance.Balance.CHEMICAL_REGISTRY_KEY;

public class Container {
    private float temperature;
    protected float heat;
    private double structurePressure;
    private final int Area;
    private final int Height;
    private final long Volume;
    private final long BasicHeatCapacity;
    private double TotalVolume;


    public Container(int height, int area, long temp, long heatCapacity) {
        Area = area;
        Height = height;
        Volume = area * height * 1000L;
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

    protected void tick() { //add updateFlowSystem() when Overriding
        updateTemp();
        updateVolume();
        updatePressure();
        updateDensity();
        updatePhaseOrder();
    }

    private void updateTemp() {
        float heatCapacity = calculateHeatCapacity();
        temperature = heat / heatCapacity;
    }

    private float calculateHeatCapacity() {
        return BasicHeatCapacity + contents.entrySet().stream()
                .map(a -> a.getKey().getHeatCapacity(a.getValue()))
                .reduce(0f, Float::sum);
    }

    private final Map<Chemical, Long> contents = new HashMap<>();
    private final EnumMap<Chemical.State, Phase> phases = new EnumMap<>(Chemical.State.class);
    private final List<Phase> sortedPhases = new ArrayList<>();
    private final List<PhaseInterface> phaseInterfaces = new ArrayList<>();

    public Phase getPhase(Chemical.State state) {
        return phases.get(state);
    }

    private Map<Chemical, Long> getChemicalMap(Chemical.State state) {
        return getPhase(state).getContents();
    }

    public double getContentVolume(Chemical.State state) {
        return getPhase(state).getVolume();
    }

    public record PressureInfo(double pressure, Phase phase) {}
    public record PhaseInterface(double pressure, Phase phaseBelow, Phase phaseAbove, double height) {}

    protected void updateFlowSystem() {
        sortedPhases.forEach(Phase::renew);
    }

    private void updateVolume() {
        sortedPhases.forEach(phase -> {
            if (phase.getState() != Chemical.State.GAS) {
                phase.updateVolume();
            }
        });
        double occupied = sortedPhases.stream()
                .filter(p -> p.getState() != Chemical.State.GAS)
                .mapToDouble(Phase::getVolume)
                .sum();
        double x = Volume - occupied;
        double a = 1;
        double gasVolume = (Math.sqrt(x * x + a) + x) / 2.0;
        getPhase(Chemical.State.GAS).setVolume(Math.max(0.0, gasVolume));
        TotalVolume = occupied + gasVolume;
    }

    private void updatePressure() {
        final int k = 1000;
        final double R = 8.314;
        final double g = 9.8;

        phaseInterfaces.clear();

        structurePressure = ((TotalVolume - Volume) * k);
        double heightRatio = Height / TotalVolume;

        double currentPressure = structurePressure + getPhase(Chemical.State.GAS).getAmountOfSubstance() * R * temperature;
        double currentHeight = Height;
        Phase lastPhase = null;
        for (Phase phase : sortedPhases) {
            if (!phase.isEmpty() || phase.getState() == Chemical.State.GAS) {
                phaseInterfaces.add(new PhaseInterface(currentPressure, phase, lastPhase, currentHeight));
                lastPhase = phase;
                currentHeight -= heightRatio * phase.getVolume();

                currentPressure += phase.getMass() * g / Area;
                phase.setPressure(currentPressure);
            }
        }
        phaseInterfaces.add(new PhaseInterface(currentPressure, null, lastPhase, 0));
    }

    private void updateDensity() {
        sortedPhases.forEach(phase -> {
            phase.updateMass();
            phase.updateDensity();
        });
    }

    private void syncStateMaps() {
        sortedPhases.forEach(Phase::clear);
        contents.forEach(this::updateStateMaps);
    }

    public void updateStateMaps(Chemical chemical, long amount) {
        Map<Chemical, Long> targetMap = getChemicalMap(chemical.state());
        if (amount > 0) targetMap.put(chemical, amount);
        if (amount == 0) targetMap.remove(chemical);
    }

    private void updatePhaseOrder() {
        sortedPhases.sort(Comparator.comparingDouble(Phase::getDensity));
    }

    public List<Phase> getSortedPhases() {
        return sortedPhases;
    }

    public List<PhaseInterface> getPhaseInterfaces(int yBottom, int yTop) {
        return phaseInterfaces.stream().filter(a -> yBottom <= a.height && a.height <= yTop).toList();
    }

    protected void changeChemical(Chemical chemical, long amount) {
        if (amount == 0) return;
        contents.merge(chemical, amount, Long::sum);
        if (contents.getOrDefault(chemical, 0L) < 0) LOGGER.error("Overcost chemicals");
        updateStateMaps(chemical, contents.get(chemical));
        if (contents.getOrDefault(chemical, 0L) == 0) contents.remove(chemical);
        markChanged();
    }

    public void changeChemical(Chemical chemical, long amount, float temperature) {
         changeChemical(chemical, amount);
        float deltaHeat = chemical.getHeatCapacity(amount) * temperature;
        heat += deltaHeat;
    }

    public long tryChangeChemical(Chemical chemical, long amount, float temperature) {
        amount = Math.min(contents.get(chemical), -amount);
        changeChemical(chemical, -amount);
        float deltaHeat = chemical.getHeatCapacity(amount) * temperature;
        heat -= deltaHeat;
        return amount;
    }

    public double getConcentration(Chemical chemical) {
        double amount = getChemicalAmount(chemical);
        return switch (chemical.state()) {
            case ORGANIC, AQUEOUS, GAS -> amount / getContentVolume(chemical.state());
            default -> 1;
        };
    }

    public PressureInfo getPressureInfo(double absoluteHeight) {
        if (phaseInterfaces.isEmpty() || absoluteHeight < 0.0 || absoluteHeight > Height) {
            return new PressureInfo(0.0, null);
        }
        for (int i = 0; i < phaseInterfaces.size() - 1; i++) {
            PhaseInterface upper = phaseInterfaces.get(i);
            PhaseInterface lower = phaseInterfaces.get(i + 1);
            if (absoluteHeight <= upper.height && absoluteHeight >= lower.height) {
                double t = (upper.height - absoluteHeight) / (upper.height - lower.height);
                double pressure = upper.pressure + t * (lower.pressure - upper.pressure);
                Phase phase = upper.phaseBelow;
                return new PressureInfo(pressure, phase);
            }
        }
        return new PressureInfo(0.0, null);
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

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Chemicals", SaveChemicals());
        tag.putFloat("Heat", heat);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("Chemicals", Tag.TAG_LIST)) {
            LoadChemicals(tag.getList("Chemicals", Tag.TAG_COMPOUND));
        }
        if (tag.contains("Heat", Tag.TAG_FLOAT)) {
            heat = tag.getFloat("Heat");
        }
    }

    public double getChemicalAmount(Chemical chemical) {
        return (double) contents.getOrDefault(chemical, 0L) / 1000;
    }

    public long getVolume() {
        return Volume;
    }

    public double getTotalVolume() {
        return TotalVolume;
    }

    public float getTemperature() {
        return temperature;
    }

    public void light() {
        temperature += 600;
    }//TODO
}

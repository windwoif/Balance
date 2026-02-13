package com.windwoif.balance.content.reactors.reactorCore;
import com.windwoif.balance.Balance;
import com.windwoif.balance.Chemical;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static com.windwoif.balance.Balance.CHEMICAL_REGISTRY_KEY;

public class Container {
    private float temperature;
    protected float heat;
    private final int Volume;
    private final int BasicHeatCapacity;

    public Container(int volume, int temp, int heatCapacity){
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
    }
    private Runnable markChangedCallback;
    public void setMarkChangedCallback(Runnable callback) {
        this.markChangedCallback = callback;
    }
    protected void markChanged() {
        if (markChangedCallback != null) markChangedCallback.run();
    }
    private static final Logger LOGGER = LogUtils.getLogger();

    public void tick(){
        updateTemp();
        updateVolume();
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

    private final Map<Chemical, Integer> contents = new HashMap<>();

    private final EnumMap<Chemical.State, Phase> phases = new EnumMap<>(Chemical.State.class);

    public Phase getPhase(Chemical.State state) {
        return phases.get(state);
    }

    private Map<Chemical, Integer> getChemicalMap(Chemical.State state) {
        return getPhase(state).getContents();
    }
    public double getContentVolume(Chemical.State state) {
        return getPhase(state).getVolume();
    }

    private void updateVolume(){
        phases.forEach((state, phase) -> phase.updateVolume());
        double occupiedVolume = phases.values().stream().mapToDouble(Phase::getVolume).sum();
        phases.get(Chemical.State.GAS).setVolume(Volume - occupiedVolume);
    }

    private void syncStateMaps() {
        phases.forEach((state, phase) -> phase.clear());
        contents.forEach(this::updateStateMaps);
    }
    public void updateStateMaps(Chemical chemical, int amount) {
        Map<Chemical, Integer> targetMap = getChemicalMap(chemical.state());
        if (amount > 0) {
            targetMap.put(chemical, amount);
        }
    }

    public void changeChemical(Chemical chemical, int amount) {
        contents.merge(chemical, amount, Integer::sum);
        if (contents.getOrDefault(chemical,0) < 0) LOGGER.error("Overcost chemicals");
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
            int amount = chemicalTag.getInt("amount");
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
                chemicalTag.putInt("amount", amount);
                chemicalsList.add(chemicalTag);
            }
        });
        return chemicalsList;
    }
    public Component testFill() {
//        changeChemical(Chemicals.NAOH.get(), 1000000);
//        changeChemical(Chemicals.HCL.get(), 1000000);
//        changeChemical(Chemicals.WATER.get(), 1000000);
        return Component.literal("filled with NO");
    }
    public Component testFill2() {
//        changeChemical(Chemicals.NO2.get(), 1000000);
//        changeChemical(Chemicals.O2.get(), 1000000);
//        changeChemical(Chemicals.NO.get(), 1000000);
        return Component.literal("filled with NO@");
    }
    public double GetChemical(Chemical chemical) {
        return (double) contents.getOrDefault(chemical, 0) / 1000;
    }
    public int getVolume() {
        return Volume;
    }
    public float getTemperature() { return temperature; }
}

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

import java.util.HashMap;
import java.util.Map;

import static com.windwoif.balance.Balance.CHEMICAL_REGISTRY_KEY;

public class Container {
    private float temperature;
    protected float heat;
    private final int Volume;
    private final int BasicHeatCapacity;
    private float heatCapacity;

    public Container(int volume, int temp, int heatCapacity){
        Volume = volume;
        BasicHeatCapacity = heatCapacity;
        heat = temp * heatCapacity;
        temperature = temp;
    }
    private Runnable markChangedCallback;
    public void setMarkChangedCallback(Runnable callback) {
        this.markChangedCallback = callback;
    }

    public void tick(){
        heatCapacity = calculateHeatCapacity();
        temperature = heat / heatCapacity;
    }

    private float calculateHeatCapacity(){
        return BasicHeatCapacity + contents.entrySet().stream()
                .map(a -> a.getKey().getHeatCapacity(a.getValue()))
                .reduce(0f, Float::sum);
    }

    protected void markChanged() {
        if (markChangedCallback != null) markChangedCallback.run();
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<Chemical, Integer> contents = new HashMap<>();
    private final Map<Chemical, Integer> solids = new HashMap<>();
    private final Map<Chemical, Integer> liquids = new HashMap<>();
    private final Map<Chemical, Integer> gases = new HashMap<>();
    private final Map<Chemical, Integer> aqueous = new HashMap<>();

    public void updateStateMaps(Chemical chemical, int amount) {
        Map<Chemical, Integer> targetMap = switch(chemical.state()) {
            case SOLID -> solids;
            case LIQUID -> liquids;
            case GAS -> gases;
            case AQUEOUS -> aqueous;
        };

        if (amount > 0) {
            targetMap.put(chemical, amount);
        }
    }
    private void syncStateMaps() {
        solids.clear(); liquids.clear(); gases.clear(); aqueous.clear();
        contents.forEach(this::updateStateMaps);
    }
    public double calculateTotalVolume() {
        double totalVolume = 0.0;
        for (Map.Entry<Chemical, Integer> entry : contents.entrySet()) {
            totalVolume += entry.getKey().getEffectiveVolume(entry.getValue());
        }
        return Math.max(totalVolume, 0.001); // At least 1 mL
    }
    public double calculateLiquidVolume() {

        double liquidVolume = 0.0;
        for (Map.Entry<Chemical, Integer> entry : liquids.entrySet()) {
            liquidVolume += entry.getKey().getEffectiveVolume(entry.getValue());
        }
        return liquidVolume;
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
    public double getConcentration(Chemical chemical) {
        double amount = GetChemical(chemical);
        if (chemical.state() == Chemical.State.LIQUID) {
            return amount / calculateLiquidVolume();
        } else if (chemical.state() == Chemical.State.GAS) {
            return amount / (Volume - calculateTotalVolume());
        }
        return 1;
    }
    public Component check() {
        if (contents.isEmpty()) return Component.literal("Empty");
        MutableComponent message = Component.literal("Content:\n");
        contents.forEach((chemical, amount) ->
                message.append(Component.literal(String.format("- %s: %d mol\n", chemical.name(), amount/1000))));
        return message;
    }
    public void changeChemical(Chemical chemical, int amount) {
        contents.merge(chemical, amount, Integer::sum);
        if (contents.getOrDefault(chemical,0) < 0) LOGGER.error("Overcost chemicals");
        updateStateMaps(chemical, contents.get(chemical));
        markChanged();
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
    public void setTemperature(int temperature) { this.temperature = temperature; }
    public float getTemperature() { return temperature; }
}

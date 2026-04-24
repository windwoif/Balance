package com.windwoif.balance.content.reactors.reactorCore;

import com.windwoif.balance.content.reactors.recipe.chemical.Chemical;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Phase {
    public Phase(Chemical.State state) {
        this.state = state;
    }
    private final Chemical.State state;
    private final Map<Chemical, Long> contents = new HashMap<>();
    private Map<Chemical, Long> RecentContents = new HashMap<>();
    private final Map<ReactorConnection, Double> connectionDemands = new HashMap<>();
    private double demandRatio = UNUPDATED;
    private double volume;
    private double density;
    private double mass;
    private double pressure;
    private static final double UNUPDATED = -1;

    public void clear(){
        contents.clear();
    }
    public boolean isEmpty(){
        return contents.isEmpty();
    }
    public Map<Chemical, Long> getContents() {
        return contents;
    }
    public void updateVolume() {
        if (state != Chemical.State.GAS) this.volume = contents.entrySet().stream()
                .mapToDouble(a -> a.getKey().getEffectiveVolume(a.getValue())).sum();
    }
    public void setVolume(double volume) {
        this.volume = volume;
    }
    public double getVolume() {
        return volume;
    }
    public void setPressure(double pressure) {
        this.pressure = pressure;
    }
    public double getPressure() {
        return pressure;
    }
    public void updateDensity() {
        density = Math.max(mass / volume, 0);
    }
    public double getDensity() {
        return density;
    }
    public void updateMass() {
        mass = contents.entrySet().stream().mapToDouble(a -> a.getKey().getMass(a.getValue())).sum();
    }
    public double getMass() {
        return mass;
    }
    public int getAmountOfSubstance() {
        return contents.values().stream().mapToInt(Long::intValue).sum() / 1000;
    }
    public int getRenderColor() {
        if (contents.isEmpty()) return 0x00000000;

        double totalMoles = getAmountOfSubstance();
        if (totalMoles <= 0) return 0x00000000;

        Chemical.State state = this.state;
        if (state == Chemical.State.SOLID || state == Chemical.State.MOLTEN_SALT || state == Chemical.State.MOLTEN_METAL) {
            double totalR = 0, totalG = 0, totalB = 0;
            for (Map.Entry<Chemical, Long> entry : contents.entrySet()) {
                Chemical chem = entry.getKey();
                double moles = entry.getValue() / 1000.0;
                double fraction = moles / totalMoles;

                int argb = chem.ARGB();
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                totalR += r * fraction;
                totalG += g * fraction;
                totalB += b * fraction;
            }
            int r = (int) Math.round(totalR);
            int g = (int) Math.round(totalG);
            int b = (int) Math.round(totalB);
            int a = 0xFF;
            return (a << 24) | (r << 16) | (g << 8) | b;
        } else {
            Map<Chemical, Double> concentrations = getConcentrations();
            double rProd = 1.0, gProd = 1.0, bProd = 1.0;
            float maxAlpha = 0.0f;

            for (Map.Entry<Chemical, Double> entry : concentrations.entrySet()) {
                if (entry.getValue() == 0) continue;
                Chemical chem = entry.getKey();
                double concentration = entry.getValue();
                int argb = chem.ARGB();
                float rBase = ((argb >> 16) & 0xFF) / 255.0f;
                float gBase = ((argb >> 8) & 0xFF) / 255.0f;
                float bBase = (argb & 0xFF) / 255.0f;
                float alphaBase = ((argb >> 24) & 0xFF) / 255.0f;

                rProd *= Math.pow(rBase, concentration);
                gProd *= Math.pow(gBase, concentration);
                bProd *= Math.pow(bBase, concentration);

                if (alphaBase > maxAlpha) maxAlpha = alphaBase;
            }

            int r = (int) (rProd * 255);
            int g = (int) (gProd * 255);
            int b = (int) (bProd * 255);
            int a = (int) (maxAlpha * 255);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }
    private Map<Chemical, Double> getConcentrations() {
        return contents.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                a -> a.getValue() / volume * 1000));
    }

    public void demand(ReactorConnection connection, double amount) {
        if (!(amount >= 0)) return;
        amount = amount * amount / (amount + 1);
        connectionDemands.merge(connection, Math.min(amount,1e5), Double::sum);
    }

    public Map<Chemical, Long> getFlowAmount(ReactorConnection connection) {
        double amount = connectionDemands.getOrDefault(connection, 0d);
        Function<Map.Entry<Chemical, Long>, Map.Entry<Chemical, Long>> mapper;
        if (demandRatio == UNUPDATED) {
            updateDemandRatio();
            mapper = a -> Map.entry(a.getKey(), (long) Math.ceil(a.getKey().getEffectiveVolume(a.getValue()) * demandRatio * amount / 2));
        } else  {
            mapper = a -> Map.entry(a.getKey(), (long) Math.floor(a.getKey().getEffectiveVolume(a.getValue()) * demandRatio * amount / 2));
        }
        return RecentContents.entrySet().stream().map(mapper).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void updateDemandRatio() {
        demandRatio = Math.min(1, volume / connectionDemands.values().stream().mapToDouble(v -> v).sum());
    }

    public void renew() {
        connectionDemands.clear();
        demandRatio = UNUPDATED;
        RecentContents = new HashMap<>(contents);
    }

    public Chemical.State getState() {
        return state;
    }

    @Override
    public String toString() {
        return  String.format("%s", state);
    }
}

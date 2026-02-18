package com.windwoif.balance.content.reactors.reactorCore;

import com.windwoif.balance.Chemical;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Phase {
    public Phase(Chemical.State state) {
        this.state = state;
    }
    private final Chemical.State state;
    private final Map<Chemical, Long> contents = new HashMap<>();
    private double volume;
    private double density;
    public void clear(){
        contents.clear();
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
    public void updateDensity() {
        density = contents.entrySet().stream().mapToDouble(a -> a.getKey().molarMass() * a.getValue() / 1000).sum() / volume;
    }
    public double getDensity() {
        return density;
    }
    public int getRenderColor() {
        if (contents.isEmpty()) return 0x00000000;
        Map<Chemical, Double> concentrations = getConcentrations();
        double rProd = 1.0, gProd = 1.0, bProd = 1.0;
        float maxAlpha = 0.0f;

        for (Map.Entry<Chemical, Double> entry : concentrations.entrySet()) {
            if (entry.getValue() == 0) continue;//TODO: delete after the react problem was fixed
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
        int a = (int) (maxAlpha * 255); // 或根据浓度调整

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    private Map<Chemical, Double> getConcentrations() {
        return contents.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                a -> a.getValue() / volume * 1000));
    }

    public Chemical.State getState() {
        return state;
    }
}

package com.windwoif.balance.content.reactors.reactorCore;

import com.windwoif.balance.Chemical;

import java.util.HashMap;
import java.util.Map;

public class Phase {
    public Phase(Chemical.State state) {
        this.state = state;
    }
    private final Chemical.State state;
    private final Map<Chemical, Long> contents = new HashMap<>();
    private double volume;
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
    public double getDensity() {
        return contents.entrySet().stream().mapToDouble(a -> a.getKey().molarMass() * a.getValue() / 1000).sum() / volume;
    }
//    public double getColor() {
//
//
//    }
}

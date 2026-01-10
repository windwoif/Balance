package com.windwoif.balance;

public record Chemical(String name, State state, int enthalpy, float entropy, float MolarVolume, float MolarHeatCapacity) {
    public enum State {
        SOLID, LIQUID, GAS, AQUEOUS
    }

    public double getGf(float temperature) {
        return enthalpy - temperature * entropy;
    }

    // Helper method to get effective volume for concentration calculations
    public float getEffectiveVolume(int amount$) { //$ for 1000 times
        return (float) ((amount$ / 1000.0) * MolarVolume);
    }
    public float getHeatCapacity(int amount$) {
        return (float) ((amount$ / 1000.0) * MolarHeatCapacity);
    }
}

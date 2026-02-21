package com.windwoif.balance.content.reactors.recipe.chemical;

public record Chemical(String name, State state, int enthalpy, float entropy, float MolarVolume, float MolarHeatCapacity, float molarMass, int ARGB) {
    public enum State {
        SOLID, LIQUID_POLAR, LIQUID_NONPOLAR, GAS, AQUEOUS, ORGANIC, MOLTEN_SALT, MOLTEN_METAL
    }

    public double getGf(float temperature) {
        return enthalpy - temperature * entropy;
    }

    // Helper method to get effective volume for concentration calculations
    public float getEffectiveVolume(long amount$) { //$ for 1000 times
        return (float) ((amount$ / 1000.0) * MolarVolume);
    }
    public float getHeatCapacity(long amount$) {
        return (float) ((amount$ / 1000.0) * MolarHeatCapacity);
    }
}

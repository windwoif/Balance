package com.windwoif.balance.content.reactors.recipe.reaction;

import com.windwoif.balance.content.reactors.recipe.chemical.Chemical;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Reaction {
    private final Map<Chemical, Integer> reactants;
    private final Map<Chemical, Integer> products;
    private final double activationEnergy;
    private final double preExponential;
    private final Map<Chemical, Integer> totalReaction;
    private static final double R = 8.314;
    public final State state;

    public Reaction(Map<Chemical, Integer> reactants, Map<Chemical, Integer> products, double activationEnergy, double preExponential, State state) {
        this.reactants = Map.copyOf(reactants);
        this.products = Map.copyOf(products);
        this.activationEnergy = activationEnergy;
        this.preExponential = preExponential;
        this.state = state;
        totalReaction = Stream
                .concat(products.keySet().stream(), reactants.keySet().stream())
                .distinct()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> products.getOrDefault(key, 0) - reactants.getOrDefault(key, 0)
                ));
    }
    public enum State {
        LIQUID_POLAR, LIQUID_NONPOLAR, GAS, SOLID
    }
    public double getEq(float T) {
        double deltaG = totalReaction.entrySet().stream()
                .map(a -> a.getKey().getGf(T) * a.getValue())
                .reduce(0.0, Double::sum);
        return Math.exp(-deltaG / (8.314 * T));
    }

    public double calculateRateConstant(float temperature){
        return preExponential * Math.exp(-activationEnergy / (R * temperature));
    }

    public Map<Chemical, Integer> getReactants() { return reactants; }
    public Map<Chemical, Integer> getProducts() { return products; }

    public Map<Chemical, Integer> getTotalReaction() {
        return totalReaction;
    }

}
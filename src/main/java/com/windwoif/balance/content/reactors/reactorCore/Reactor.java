package com.windwoif.balance.content.reactors.reactorCore;

import com.windwoif.balance.Balance;
import com.windwoif.balance.Chemical;
import com.windwoif.balance.Reaction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Reactor extends Container {

    private List<Reaction> usableReactions = new ArrayList<>(RegistryManager.ACTIVE.getRegistry(Balance.REACTION_REGISTRY_KEY).getValues());

    public Reactor(int volume,  int temperature, int heatCapacity) {
        super(volume, temperature, heatCapacity);
    }

    public void tick(float timeStep) {
        tick();
        Map<Chemical, Integer> finalChange = getFinalChange(getTotalReactPlan(timeStep));
        finalChange.forEach(this::changeChemical);
        int heatChange = finalChange.entrySet().stream()
                .mapToInt(entry -> entry.getKey().enthalpy() * entry.getValue())
                .sum();
        heat += heatChange;
        markChanged();
    }

    private Map<Reaction, Double> getReactPlan(List<Reaction> stepReactions, float time) {
        double liquidVolume = calculateLiquidVolume();
        double gasVolume = getVolume() - calculateTotalVolume();
        return stepReactions.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        reaction -> {
                            double k_forwardRate = reaction.calculateRateConstant(getTemperature());
                            double equilibrium = reaction.getEq(getTemperature());
                            double forwardRate = calculatePartRate(k_forwardRate, reaction.getReactants());
                            double backwardRate = calculatePartRate(k_forwardRate / equilibrium, reaction.getProducts());
                            double totalRate = (forwardRate - backwardRate) * time * switch(reaction.state) {
                                case LIQUID -> liquidVolume;
                                case GAS -> gasVolume;
                            };

                            return totalRate;
                        }
                ));
    }

    private double calculatePartRate(double baseRate, Map<Chemical, Integer> components) {
        return components.entrySet().stream()
                .mapToDouble(entry -> Math.pow(getConcentration(entry.getKey()), entry.getValue()))
                .reduce(baseRate, (a, b) -> a * b);
    }

    private Map<Chemical, Double> getTotalChange(@NotNull Map<Reaction, Double> reactPlan){
        return reactPlan.entrySet().stream()
                .flatMap(reactionEntry -> reactionEntry.getKey().getTotalReaction().entrySet().stream()
                        .map(entry -> Map.entry(entry.getKey(), entry.getValue() * reactionEntry.getValue())))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.summingDouble(Map.Entry::getValue)));
    }
    private Map<Chemical, Integer> getFinalChange(@NotNull Map<Reaction, Integer> reactPlan){
        return reactPlan.entrySet().stream()
                .flatMap(reactionEntry -> reactionEntry.getKey().getTotalReaction().entrySet().stream()
                        .map(entry -> Map.entry(entry.getKey(), entry.getValue() * reactionEntry.getValue())))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.summingInt(Map.Entry::getValue)));
    }

    private Map.Entry<Chemical, Double> getLackingChemical(@NotNull Map<Reaction, Double> reactPlan) {
        return getTotalChange(reactPlan).entrySet().stream()

                .filter(entry -> entry.getValue() < 0)
                .map(entry -> Map.entry(entry.getKey(),
                        - GetChemical(entry.getKey()) / entry.getValue()))
                .filter(entry -> entry.getValue() < 1)
                .min(Map.Entry.comparingByValue()).orElse(null);
    }

    private Map<Reaction, Double> getNewReactPlan(Map.Entry<Chemical, Double> lackingChemical, Map<Reaction, Double> currentReactPlan){
        return currentReactPlan.entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .filter(entry -> !(entry.getValue() > 0 ?
                        entry.getKey().getReactants().containsKey(lackingChemical.getKey()) :
                        entry.getKey().getProducts().containsKey(lackingChemical.getKey())))
                .map(entry -> Map.entry(entry.getKey(), entry.getValue() * (1 - lackingChemical.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Reaction, Double> collectReactSteps(Map<Reaction, Double> currentReactPlan, Map<Reaction, Double> reactPlan, Double ratio){
        return Stream.concat(currentReactPlan.entrySet().stream(), reactPlan.entrySet().stream()
                        .map(chemicalEntry -> Map.entry(chemicalEntry.getKey(),
                                chemicalEntry.getValue() * ratio)
                        )
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Double::sum
                ));
    }

    public Map<Reaction, Integer> getTotalReactPlan(float time){
        Map<Reaction, Double> totalReactPlan = new HashMap<>();
        Map<Reaction, Double> reactPlan = getReactPlan(usableReactions, time);

        for (int i = 0; i < usableReactions.size(); i++) {
            Map.Entry<Chemical, Double> lackingChemical = getLackingChemical(reactPlan);

            if (lackingChemical == null) {
                totalReactPlan = collectReactSteps(totalReactPlan, reactPlan, 1.0);
                break;
            }
            totalReactPlan = collectReactSteps(totalReactPlan, reactPlan,  lackingChemical.getValue());
            reactPlan = getNewReactPlan(lackingChemical, reactPlan);
        }
        return totalReactPlan.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (int)(entry.getValue() * 1000)
                ));
    }



    public List<Reaction> getUsableReactions() { return usableReactions; }


    public Component displayReactionPlan() {
        Map<Reaction, Integer> reactionPlan = getTotalReactPlan(0.05f);
        if (reactionPlan.isEmpty()) {
            return Component.literal("No reaction plans");
        }

        MutableComponent message = Component.literal("Reaction Plans:\n");

        IForgeRegistry<Reaction> reactionRegistry = RegistryManager.ACTIVE.getRegistry(Balance.REACTION_REGISTRY_KEY);

        for (Map.Entry<Reaction, Integer> entry : reactionPlan.entrySet()) {
            Reaction reaction = entry.getKey();
            Integer amount = entry.getValue();


            ResourceLocation reactionId = reactionRegistry.getKey(reaction);
            String reactionName = reactionId != null ? reactionId.toString() : "Unknown Reaction";

            message.append(Component.literal(
                    String.format("- %s: %d mol\n", reactionName, amount)
            ));
        }

        return message;
    }

}
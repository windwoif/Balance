package com.windwoif.balance.content.reactors.recipe.reaction;

import com.windwoif.balance.content.reactors.recipe.chemical.Chemicals;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

import static com.windwoif.balance.Balance.REACTIONS;
import static com.windwoif.balance.content.reactors.recipe.chemical.Chemicals.*;
import static com.windwoif.balance.content.reactors.recipe.reaction.Reaction.State.*;

public class Reactions {

    public static void init() {}

    public static final RegistryObject<Reaction> NULL_REACTION = REACTIONS.register("none_reaction",
            () -> new Reaction(
                    Map.of(Chemicals.NULL_CHEMICAL.get(), 1),
                    Map.of(Chemicals.NULL_CHEMICAL.get(), 1),
                    1,
                    1,
                    GAS
            )
    );
    // ---------- 中和反应（离子方程式）----------
    public static final RegistryObject<Reaction> NEUTRALIZATION = REACTIONS.register("neutralization",
            () -> new Reaction(
                    Map.of(                               // 反应物
                            HYDROGEN_ION.get(), 1,
                            HYDROXIDE.get(), 1
                    ),
                    Map.of(                               // 产物
                            WATER.get(), 1
                    ),
                    10000.0,      // 活化能 10 kJ/mol
                    1.0e6,        // 指前因子
                    LIQUID_POLAR        // 液相反应
            ));

    // ---------- 甲烷燃烧 ----------
    public static final RegistryObject<Reaction> METHANE_COMBUSTION = REACTIONS.register("methane_combustion",
            () -> new Reaction(
                    Map.of(
                            METHANE.get(), 1,
                            OXYGEN.get(), 2
                    ),
                    Map.of(
                            CARBON_DIOXIDE.get(), 1,
                            WATER.get(), 2
                    ),
                    50000.0,      // 50 kJ/mol
                    1.0e8,        // 较快的气相反应
                    GAS
            ));

    // ---------- 乙醇燃烧 ----------
    public static final RegistryObject<Reaction> ETHANOL_COMBUSTION = REACTIONS.register("ethanol_combustion",
            () -> new Reaction(
                    Map.of(
                            ETHANOL.get(), 1,
                            OXYGEN.get(), 3
                    ),
                    Map.of(
                            CARBON_DIOXIDE.get(), 2,
                            WATER.get(), 3
                    ),
                    55000.0,
                    1.0e8,
                    GAS
            ));

    // ---------- 氯化钠溶解（可逆过程，仅演示）----------
    public static final RegistryObject<Reaction> NACL_DISSOLUTION = REACTIONS.register("nacl_dissolution",
            () -> new Reaction(
                    Map.of(
                            SODIUM_CHLORIDE.get(), 1
                    ),
                    Map.of(
                            SODIUM_ION.get(), 1,
                            CHLORIDE.get(), 1
                    ),
                    20000.0,      // 溶解活化能
                    1.0e3,
                    LIQUID_POLAR        // 溶液环境
            ));

}



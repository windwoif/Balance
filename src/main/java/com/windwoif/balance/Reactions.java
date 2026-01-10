package com.windwoif.balance;

import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

import static com.windwoif.balance.Balance.REACTIONS;
import static com.windwoif.balance.Chemicals.*;
import static com.windwoif.balance.Reaction.State.*;

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

}



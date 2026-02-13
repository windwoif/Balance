package com.windwoif.balance;

import net.minecraftforge.registries.RegistryObject;

import static com.windwoif.balance.Balance.CHEMICALS;

public class Chemicals {

    public static final RegistryObject<Chemical> NULL_CHEMICAL = CHEMICALS.register("null_chemical",
            () -> new Chemical("null_chemical", Chemical.State.GAS, 0, 0, 0, 0, 0));


    public static void init() {}
}

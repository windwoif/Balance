package com.windwoif.balance;

import net.minecraftforge.registries.RegistryObject;

import static com.windwoif.balance.Balance.CHEMICALS;
import static com.windwoif.balance.Chemical.State.*;

public class Chemicals {
    public static void init() {}

    // 占位物质（无色）
    public static final RegistryObject<Chemical> NULL_CHEMICAL = CHEMICALS.register("null_chemical",
            () -> new Chemical("null_chemical", GAS, 0, 0, 0, 0, 0, 0));

    // ---------- 水溶液离子----------
    public static final RegistryObject<Chemical> HYDROGEN_ION = CHEMICALS.register("hydrogen_ion",
            () -> new Chemical("H⁺", AQUEOUS, 0, 0f, 0.001f, 0f, 1.0f, 0));

    public static final RegistryObject<Chemical> HYDROXIDE = CHEMICALS.register("hydroxide",
            () -> new Chemical("OH⁻", AQUEOUS, -230000, -10.8f, 0.001f, 0f, 17.0f, 0));

    public static final RegistryObject<Chemical> SODIUM_ION = CHEMICALS.register("sodium_ion",
            () -> new Chemical("Na⁺", AQUEOUS, -240100, 58.5f, 0.001f, 0f, 23.0f, 0));

    public static final RegistryObject<Chemical> CHLORIDE = CHEMICALS.register("chloride",
            () -> new Chemical("Cl⁻", AQUEOUS, -167200, 56.5f, 0.001f, 0f, 35.5f, 0));

    // ---------- 分子化合物----------
    public static final RegistryObject<Chemical> WATER = CHEMICALS.register("water",
            () -> new Chemical("H₂O", LIQUID_POLAR, -285800, 70.0f, 0.018f, 75.3f, 18.0f, 0x803F76E4));

    public static final RegistryObject<Chemical> METHANE = CHEMICALS.register("methane",
            () -> new Chemical("CH₄", GAS, -74600, 186.3f, 22.4f, 35.7f, 16.0f, 0));

    public static final RegistryObject<Chemical> OXYGEN = CHEMICALS.register("oxygen",
            () -> new Chemical("O₂", GAS, 0, 205.0f, 22.4f, 29.4f, 32.0f, 0));

    public static final RegistryObject<Chemical> CARBON_DIOXIDE = CHEMICALS.register("carbon_dioxide",
            () -> new Chemical("CO₂", GAS, -393500, 213.8f, 22.4f, 37.1f, 44.0f, 0));

    public static final RegistryObject<Chemical> ETHANOL = CHEMICALS.register("ethanol",
            () -> new Chemical("C₂H₅OH", ORGANIC, -277600, 160.7f, 0.058f, 112.0f, 46.0f, 0));

    // ---------- 固体 ----------
    public static final RegistryObject<Chemical> SODIUM_CHLORIDE = CHEMICALS.register("sodium_chloride",
            () -> new Chemical("NaCl", SOLID, -411200, 72.1f, 0.027f, 50.5f, 58.5f, 0xFFFFFFFF)); // 白色
}
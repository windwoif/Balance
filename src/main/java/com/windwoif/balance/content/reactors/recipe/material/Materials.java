package com.windwoif.balance.content.reactors.recipe.material;

import com.windwoif.balance.Balance;
import com.windwoif.balance.content.reactors.recipe.chemical.Chemical;
import com.windwoif.balance.content.reactors.recipe.chemical.Chemicals;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

public class Materials {
    public static void init() {}

    private static final long NUGGET_BASE = 100000;

    public static final RegistryObject<Material> IRON = Balance.MATERIALS.register("iron",
            () -> new Material("iron",
                    phase -> phase.getState() == Chemical.State.MOLTEN_METAL,
                    Map.of(Chemicals.IRON.get(), NUGGET_BASE)
            ));

    public static final RegistryObject<Material> GOLD = Balance.MATERIALS.register("gold",
            () -> new Material("gold",
                    phase -> phase.getState() == Chemical.State.MOLTEN_METAL,
                    Map.of(Chemicals.GOLD.get(), NUGGET_BASE)
            ));

    public static final RegistryObject<Material> COPPER = Balance.MATERIALS.register("copper",
            () -> new Material("copper",
                    phase -> phase.getState() == Chemical.State.MOLTEN_METAL,
                    Map.of(Chemicals.COPPER.get(), NUGGET_BASE)
            ));
    public static final RegistryObject<Material> TEST = Balance.MATERIALS.register("redstone",
            () -> new Material("redstone",
                    phase -> phase.getState() == Chemical.State.GAS,
                    Map.of(
                            Chemicals.OXYGEN.get(), 1000L,
                            Chemicals.METHANE.get(), 500L
                    )
            ));
    public static final RegistryObject<Material> TEST2 = Balance.MATERIALS.register("lapis",
            () -> new Material("lapis",
                    phase -> phase.getState() == Chemical.State.MOLTEN_METAL,
                    Map.of(Chemicals.WATER.get(), NUGGET_BASE)));
    public static final RegistryObject<Material> TEST3 = Balance.MATERIALS.register("quartz",
            () -> new Material("quartz",
                    phase -> phase.getState() == Chemical.State.MOLTEN_METAL,
                    Map.of(Chemicals.SODIUM_CHLORIDE.get(), NUGGET_BASE)));
}

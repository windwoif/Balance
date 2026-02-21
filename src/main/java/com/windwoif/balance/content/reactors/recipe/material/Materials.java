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
}

package com.windwoif.balance.content.reactors.recipe.material;

import com.windwoif.balance.Balance;
import com.windwoif.balance.content.reactors.recipe.chemical.Chemical;
import com.windwoif.balance.content.reactors.reactorCore.Phase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.windwoif.balance.Balance.MODID;

public record Material(
        String name, //tag name
        Predicate<Phase> requirements,
        Map<Chemical, Long> content) {
    public final static Map<String, Integer> FORMS = Map.of(
            "nuggets", 1,
            "ingots", 9,
            "storage_blocks", 81
    );


    public static Optional<Map<Chemical, Long>> resolveTagToChemicals(TagKey<Item> tag) {
        String path = tag.location().getPath(); // "ingots/copper"
        String[] parts = path.split("/");
        String form;
        String materialName;
        if (parts.length == 2){
            form = parts[0]; // "ingots"
            materialName = parts[1]; // "copper"
        } else {
            return Optional.empty();
        }
        IForgeRegistry<Material> materialRegistry = RegistryManager.ACTIVE.getRegistry(Balance.MATERIAL_REGISTRY_KEY);
        Material material = materialRegistry.getValue(new ResourceLocation(MODID,materialName));
        if (material == null) return Optional.empty();

        int factor = FORMS.get(form);
        Map<Chemical, Long> result = new HashMap<>();
        material.content().forEach((chem, amount) -> result.put(chem, amount * factor));
        return Optional.of(result);
    }
}

package com.windwoif.balance;


import com.windwoif.balance.content.reactors.recipe.chemical.Chemical;
import com.windwoif.balance.content.reactors.recipe.material.Material;
import com.windwoif.balance.content.reactors.recipe.reaction.Reaction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import static com.windwoif.balance.Balance.MODID;

public class RegistryBuilders {
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onNewRegistry(NewRegistryEvent event) {
            event.create(new RegistryBuilder<Chemical>()
                    .setName(Balance.CHEMICAL_REGISTRY_KEY.location())
                    .setDefaultKey(ResourceLocation.fromNamespaceAndPath(MODID, "none_chemical"))
                    .hasTags()
            );
            event.create(new RegistryBuilder<Reaction>()
                    .setName(Balance.REACTION_REGISTRY_KEY.location())
                    .setDefaultKey(ResourceLocation.fromNamespaceAndPath(MODID, "none_reaction"))
                    .hasTags()
            );
            event.create(new RegistryBuilder<Material>()
                    .setName(Balance.MATERIAL_REGISTRY_KEY.location())
                    .setDefaultKey(ResourceLocation.fromNamespaceAndPath(MODID, "none_material"))
                    .hasTags()
            );
        }
    }
}
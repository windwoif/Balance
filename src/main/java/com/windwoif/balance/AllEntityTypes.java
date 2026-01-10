package com.windwoif.balance;

import com.windwoif.balance.content.reactors.reactorCore.ReactorEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;

import static com.windwoif.balance.Balance.ENTITY_TYPES;

public class AllEntityTypes {
    public static void init() {}
    public static final RegistryObject<EntityType<ReactorEntity>> REACTOR_ENTITY =
            ENTITY_TYPES.register("reactor",
                    () -> EntityType.Builder.<ReactorEntity>of(ReactorEntity::new,
                                    MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .clientTrackingRange(10)
                            .updateInterval(Integer.MAX_VALUE)
                            .build("reactor")
            );
}

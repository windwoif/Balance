package com.windwoif.balance;

import com.mojang.datafixers.DSL;
import com.windwoif.balance.content.reactors.debugReactor.DebugBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

import static com.windwoif.balance.Balance.BLOCK_ENTITIES;

public class AllBlockEntityTypes {
    public static void init() {}
    public static final RegistryObject<BlockEntityType<DebugBlockEntity>> DEBUG_REACTOR_ENTITY = BLOCK_ENTITIES.register("debug_reactor_entity",
            () -> BlockEntityType.Builder.of(DebugBlockEntity::new, AllBlocks.DEBUG_REACTOR.get()).build(DSL.remainderType()));
}

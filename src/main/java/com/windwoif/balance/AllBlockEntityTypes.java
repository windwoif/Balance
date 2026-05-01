package com.windwoif.balance;

import com.mojang.datafixers.DSL;
import com.windwoif.balance.content.reactors.debugReactor.DebugBlockEntity;
import com.windwoif.balance.content.valve.ValveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

import static com.windwoif.balance.AllBlocks.VALVE_BLOCK;
import static com.windwoif.balance.Balance.BLOCK_ENTITIES;

public class AllBlockEntityTypes {
    public static void init() {}
    public static final RegistryObject<BlockEntityType<DebugBlockEntity>> DEBUG_REACTOR_ENTITY = BLOCK_ENTITIES.register("debug_reactor_entity",
            () -> BlockEntityType.Builder.of(DebugBlockEntity::new, AllBlocks.DEBUG_REACTOR.get()).build(DSL.remainderType()));
    public static final RegistryObject<BlockEntityType<ValveBlockEntity>> VALVE = BLOCK_ENTITIES.register("valve",
            () -> BlockEntityType.Builder.of(ValveBlockEntity::new, VALVE_BLOCK.get()).build(null));
}

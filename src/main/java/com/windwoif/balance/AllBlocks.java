package com.windwoif.balance;

import com.windwoif.balance.content.reactors.debugReactor.DebugBlock;
import com.windwoif.balance.content.valve.ValveBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.windwoif.balance.Balance.BLOCKS;


@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AllBlocks {
    public static void init() {}
    public static final RegistryObject<Block> DEBUG_REACTOR = BLOCKS.register("debug_reactor", DebugBlock::new);
    public static final RegistryObject<Block> VALVE_BLOCK = BLOCKS.register("valve",
            () -> new ValveBlock(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion()));

}

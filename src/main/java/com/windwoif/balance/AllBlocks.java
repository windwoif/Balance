package com.windwoif.balance;

import com.windwoif.balance.content.reactors.debugReactor.DebugBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.windwoif.balance.Balance.BLOCKS;


@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AllBlocks {
    public static void init() {}
    public static final RegistryObject<Block> DEBUG_REACTOR = BLOCKS.register("debug_reactor", DebugBlock::new);


}

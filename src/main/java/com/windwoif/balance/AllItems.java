package com.windwoif.balance;

import com.windwoif.balance.content.wrench.WrenchItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import static com.windwoif.balance.Balance.ITEMS;

public class AllItems {
    public static void init() {}
    public static final RegistryObject<Item> DEBUG_REACTOR_ITEM = ITEMS.register("debug_reactor",
            () -> new BlockItem(AllBlocks.DEBUG_REACTOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> WRENCH_ITEM = ITEMS.register("wrench_item",
            () -> new WrenchItem(new Item.Properties().durability(100)));
}

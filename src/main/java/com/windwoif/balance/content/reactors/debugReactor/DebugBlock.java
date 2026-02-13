package com.windwoif.balance.content.reactors.debugReactor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class DebugBlock extends Block implements EntityBlock {
    public DebugBlock() {
        super(Properties.of()
                .strength(2.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, blockState, t) -> {
                if (t instanceof DebugBlockEntity debugBlockEntity) {
                    debugBlockEntity.tick();
                }
            };
        }
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DebugBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            ItemStack itemInHand = player.getItemInHand(hand);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DebugBlockEntity tank) {
                if (itemInHand.is(Items.DIAMOND)) {
                    Component message = tank.check2();
                    player.displayClientMessage(message, false);
                    return InteractionResult.SUCCESS;
                }
//                else if (itemInHand.is(Items.STICK)) {
//                    Component message = tank.check1();
//                    player.displayClientMessage(message, false);
//                    return InteractionResult.SUCCESS;
//                }
                else if (itemInHand.is(Items.WATER_BUCKET)) {
                    Component message = tank.fillTest();
                    player.displayClientMessage(message, false);
                    return InteractionResult.SUCCESS;
                }
                else if (itemInHand.is(Items.MILK_BUCKET)) {
                    Component message = tank.fillWithWater();
                    player.displayClientMessage(message, false);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}

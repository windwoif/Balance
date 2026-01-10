package com.windwoif.balance.content.wrench;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.windwoif.balance.content.reactors.reactorCore.ReactorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.List;

public class ReactorSelectionPacket extends SimplePacketBase {

    private BlockPos from;
    private BlockPos to;

    public ReactorSelectionPacket(BlockPos from, BlockPos to) {
        this.from = from;
        this.to = to;
    }

    public ReactorSelectionPacket(FriendlyByteBuf buffer) {
        from = buffer.readBlockPos();
        to = buffer.readBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(from);
        buffer.writeBlockPos(to);
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            double range = player.getAttribute(ForgeMod.BLOCK_REACH.get()).getValue() + 2;
            if (player.distanceToSqr(Vec3.atCenterOf(to)) > range * range)
                return;
            if (!to.closerThan(from, 25))
                return;

            AABB selectionBox = new AABB(from, to).expandTowards(1, 1, 1);

            if (checkOverlapWithReactors(player.serverLevel(), selectionBox)) {
                return;
            }

            ReactorEntity reactor = new ReactorEntity(player.level(), selectionBox);
            player.level().addFreshEntity(reactor);
        });
        return true;
    }

    private boolean checkOverlapWithReactors(ServerLevel level, AABB box) {
        List<ReactorEntity> existingReactors = level.getEntitiesOfClass(
                ReactorEntity.class,
                box.inflate(0.5)
        );

        for (ReactorEntity reactor : existingReactors) {
            if (reactor.getBoundingBox().intersects(box)) {
                return true;
            }
        }
        return false;
    }
}

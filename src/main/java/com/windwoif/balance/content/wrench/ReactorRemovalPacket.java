package com.windwoif.balance.content.wrench;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.windwoif.balance.content.reactors.reactorCore.ReactorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

public class ReactorRemovalPacket extends SimplePacketBase {

    private int entityId;
    private BlockPos soundSource;

    public ReactorRemovalPacket(int id, BlockPos soundSource) {
        this.entityId = id;
        this.soundSource = soundSource;
    }

    public ReactorRemovalPacket(FriendlyByteBuf buffer) {
        entityId = buffer.readInt();
        soundSource = buffer.readBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeBlockPos(soundSource);
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            Entity entity = player.level().getEntity(entityId);
            if (!(entity instanceof ReactorEntity reactor)) // 改为ReactorEntity
                return;
            double range = 32;
            if (player.distanceToSqr(reactor.position()) > range * range)
                return;
            if (player != null) {
                player.level().playSound(null, soundSource, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.75f, 1);
            }
            entity.discard();
        });
        return true;
    }
}

package com.windwoif.balance.network;

import com.windwoif.balance.Balance;
import com.windwoif.balance.content.wrench.ReactorSelectionPacket;
import com.windwoif.balance.content.wrench.ReactorRemovalPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class BalanceNetwork {
    private static final String PROTOCOL_VERSION = "1.0.0";

    // 创建网络通道
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Balance.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // 注册所有数据包
    public static void registerPackets() {
        int packetId = 0;

        CHANNEL.registerMessage(
                packetId++,
                ReactorSelectionPacket.class,
                ReactorSelectionPacket::write,
                ReactorSelectionPacket::new,
                (packet, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    packet.handle(context);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                packetId++,
                ReactorRemovalPacket.class,
                ReactorRemovalPacket::write,
                ReactorRemovalPacket::new,
                (packet, contextSupplier) -> packet.handle(contextSupplier.get()),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
}
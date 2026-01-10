package com.windwoif.balance.client;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.chainConveyor.ChainPackageInteractionHandler;
import com.simibubi.create.content.trains.track.CurvedTrackInteraction;
import com.windwoif.balance.Balance;
import com.windwoif.balance.content.wrench.ReactorSelectionHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Balance.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BalanceClient {
    public static final ReactorSelectionHandler REACTOR_SELECTION_HANDLER = new ReactorSelectionHandler();
    // 1. 订阅客户端Tick事件，用于每帧更新状态和渲染
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.ClientTickEvent.Phase.END) {
            REACTOR_SELECTION_HANDLER.tick();
        }
    }

    // 2. 订阅鼠标输入事件，用于处理右键和左键点击
    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        KeyMapping key = event.getKeyMapping();
        if (mc.screen != null)
            return;

        if (CurvedTrackInteraction.onClickInput(event)) {
            event.setCanceled(true);
            return;
        }
        if (key == mc.options.keyUse || key == mc.options.keyAttack) {
            if (REACTOR_SELECTION_HANDLER.onMouseInput(key == mc.options.keyAttack))
                event.setCanceled(true);
        }

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (ChainPackageInteractionHandler.onUse())
                event.setCanceled(true);
        });
    }

}

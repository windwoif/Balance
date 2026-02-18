package com.windwoif.balance.content.wrench;

import com.google.common.base.Objects;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.windwoif.balance.content.reactors.reactorCore.ReactorEntity;
import com.windwoif.balance.network.BalanceNetwork;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.List;
import java.util.Optional;

public class ReactorSelectionHandler {

    private static final int PASSIVE = 0xf9ca0d;
    private static final int HIGHLIGHT = 0xf9f415;
    private static final int FAIL = 0xc52720;

    private Object clusterOutlineSlot = new Object();
    private Object bbOutlineSlot = new Object();
    private int clusterCooldown;

    private BlockPos firstPos;
    private BlockPos hoveredPos;
    private AABB currentSelectionBox;
    private boolean selectionValid;

    private ReactorEntity selected;
    private BlockPos soundSourceForRemoval;

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        BlockPos hovered = null;
        ItemStack stack = player.getMainHandItem();
        if (!isWrench(stack)) {
            if (firstPos != null)
                discard();
            return;
        }

        if (clusterCooldown > 0) {
            if (clusterCooldown == 25)
                player.displayClientMessage(CommonComponents.EMPTY, true);
            Outliner.getInstance().keep(clusterOutlineSlot);
            clusterCooldown--;
        }

        AABB scanArea = player.getBoundingBox()
                .inflate(32, 16, 32);

        List<ReactorEntity> reactorsNearby = null;
        if (mc.level != null) {
            reactorsNearby = mc.level.getEntitiesOfClass(ReactorEntity.class, scanArea);
        }
        selected = null;
        if (firstPos == null) {
            double range = player.getAttribute(ForgeMod.BLOCK_REACH.get())
                    .getValue() + 1;
            Vec3 traceOrigin = player.getEyePosition();
            Vec3 traceTarget = RaycastHelper.getTraceTarget(player, range, traceOrigin);

            double bestDistance = Double.MAX_VALUE;
            if (reactorsNearby == null) return;
            for (ReactorEntity reactor : reactorsNearby) {
                Optional<Vec3> clip = reactor.getBoundingBox()
                        .clip(traceOrigin, traceTarget);
                if (clip.isEmpty())
                    continue;
                Vec3 vec3 = clip.get();
                double distanceToSqr = vec3.distanceToSqr(traceOrigin);
                if (distanceToSqr > bestDistance)
                    continue;
                selected = reactor;
                soundSourceForRemoval = BlockPos.containing(vec3);
                bestDistance = distanceToSqr;
            }


            for (ReactorEntity reactor : reactorsNearby) {
                boolean h = clusterCooldown == 0 && reactor == selected;
                AllSpecialTextures faceTex = h ? AllSpecialTextures.SELECTION : null;
                Outliner.getInstance().showAABB(reactor, reactor.getBoundingBox())
                        .colored(h ? HIGHLIGHT : PASSIVE)
                        .withFaceTextures(faceTex, faceTex)
                        .disableLineNormals()
                        .lineWidth(h ? 1 / 16f : 1 / 32f);
            }
        }

        HitResult hitResult = mc.hitResult;
        BlockHitResult blockHit = null;
        if (hitResult != null && hitResult.getType() == Type.BLOCK) {
            blockHit = (BlockHitResult) hitResult;
        }
        if (blockHit != null) {
            hovered = blockHit.getBlockPos().relative(blockHit.getDirection());
        }

        if (hovered == null) {
            hoveredPos = null;
            return;
        }

        if (firstPos != null && !firstPos.closerThan(hovered, 24)) {
            CreateLang.translate("super_glue.too_far")
                    .color(FAIL)
                    .sendStatus(player);
            return;
        }

        boolean cancel = player.isShiftKeyDown();
        if (cancel && firstPos == null)
            return;

        currentSelectionBox = getCurrentSelectionBox();

        boolean unchanged = Objects.equal(hovered, hoveredPos);

        if (unchanged && currentSelectionBox != null) {
            selectionValid = true;
            if (reactorsNearby != null) {
                for (ReactorEntity reactor : reactorsNearby) {
                    if (reactor.getBoundingBox().intersects(currentSelectionBox)) {
                        selectionValid = false;
                        break;
                    }
                }
            }

            int color = HIGHLIGHT;
            String key = "reactor.click_to_confirm";

            if (!selectionValid) {
                color = FAIL;
                key = "reactor.overlaps_existing";
            } else if (cancel) {
                color = FAIL;
                key = "reactor.click_to_discard";
            }

            CreateLang.translate(key)
                    .color(color)
                    .sendStatus(player);

            Outliner.getInstance().showAABB(bbOutlineSlot, currentSelectionBox)
                    .colored(selectionValid && !cancel ? HIGHLIGHT : FAIL)
                    .withFaceTextures(AllSpecialTextures.SELECTION, AllSpecialTextures.SELECTION)
                    .disableLineNormals()
                    .lineWidth(1 / 16f);

            return;
        }

        hoveredPos = hovered;
    }

    private boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof WrenchItem;
    }

    private AABB getCurrentSelectionBox() {
        return firstPos == null || hoveredPos == null ? null :
                new AABB(firstPos, hoveredPos).expandTowards(1, 1, 1);
    }

    public boolean onMouseInput(boolean attack) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null) return false;
        if (!isWrench(player.getMainHandItem()))
            return false;
        if (!player.mayBuild())
            return false;

        if (attack) {
            if (selected == null)
                return false;
            BalanceNetwork.CHANNEL.sendToServer(new ReactorRemovalPacket(selected.getId(), soundSourceForRemoval));
            selected = null;
            clusterCooldown = 0;
            return true;
        }

        if (player.isShiftKeyDown()) {
            if (firstPos != null) {
                discard();
                return true;
            }
            return false;
        }

        if (hoveredPos == null)
            return false;

        Direction face = null;
        if (mc.hitResult instanceof BlockHitResult bhr) {
            face = bhr.getDirection();
        }

        if (firstPos != null && currentSelectionBox != null) {
            if (!selectionValid)
                return true;
            if (mc.hitResult instanceof BlockHitResult bhr) {
                face = bhr.getDirection();
            }

            WrenchItem.spawnParticles(level, hoveredPos, face, true);

            confirm();
            return true;
        }

        firstPos = hoveredPos;
        if (face != null)
            WrenchItem.spawnParticles(level, firstPos, face, true);
        CreateLang.translate("reactor.first_pos")
                .sendStatus(player);
        if (level != null) {
            level.playSound(player, firstPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.75f, 1);
        }
        return true;
    }

    public void discard() {
        LocalPlayer player = Minecraft.getInstance().player;
        currentSelectionBox = null;
        selectionValid = false;
        firstPos = null;
        CreateLang.translate("reactor.abort")
                .sendStatus(player);
        clusterCooldown = 0;
    }

    public void confirm() {
        LocalPlayer player = Minecraft.getInstance().player;

        BalanceNetwork.CHANNEL.sendToServer(new ReactorSelectionPacket(firstPos, hoveredPos));
        if (player != null) {
            player.level().playSound(player, hoveredPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.75f, 1);
        }

        if (currentSelectionBox != null) {
            Outliner.getInstance().showAABB(clusterOutlineSlot, currentSelectionBox)
                    .colored(0xffd00d)
                    .withFaceTextures(AllSpecialTextures.SELECTION, AllSpecialTextures.HIGHLIGHT_CHECKERED)
                    .disableLineNormals()
                    .lineWidth(1 / 24f);
        }

        discard();
        CreateLang.translate("reactor.success")
                .sendStatus(player);
        clusterCooldown = 40;
    }
}
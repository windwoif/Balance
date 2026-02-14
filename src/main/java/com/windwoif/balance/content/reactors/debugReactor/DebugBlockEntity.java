package com.windwoif.balance.content.reactors.debugReactor;

import com.windwoif.balance.AllBlockEntityTypes;
import com.windwoif.balance.Balance;
import com.windwoif.balance.Reaction;
import com.windwoif.balance.content.reactors.reactorCore.Reactor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.ArrayList;
import java.util.List;

import static com.windwoif.balance.dimension.DimensionAtmosphere.getAtmosphere;

public class DebugBlockEntity extends BlockEntity {
    private final com.windwoif.balance.content.reactors.reactorCore.Reactor reactor;

    public DebugBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntityTypes.DEBUG_REACTOR_ENTITY.get(), pos, state);
        reactor = new Reactor(1000000 , level != null ? getAtmosphere(level.dimension()).temperature() : 0, 10000);
        reactor.setMarkChangedCallback(this::setChanged);
        IForgeRegistry<Reaction> reactionRegistry = RegistryManager.ACTIVE.getRegistry(Balance.REACTION_REGISTRY_KEY);
        if (reactionRegistry != null) {
            availableReactions.addAll(reactionRegistry.getValues());
        }
    }

    private List<Reaction> availableReactions = new ArrayList<>();

    public void tick() {
        if (level != null && !level.isClientSide()) {
            reactor.tick(0.05f);


//            if (reactor.getContentVolume(Chemical.State.GAS) <= 0) {
//                level.explode(
//                        null,
//                        getBlockPos().getX() + 0.5,
//                        getBlockPos().getY() + 0.5,
//                        getBlockPos().getZ() + 0.5,
//                        4.0f,
//                        Level.ExplosionInteraction.BLOCK
//                );
//            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag chemicalsList = reactor.SaveChemicals();
        tag.put("Chemicals", chemicalsList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Chemicals", Tag.TAG_LIST)) {
            ListTag chemicalsList = tag.getList("Chemicals", Tag.TAG_COMPOUND);
            reactor.LoadChemicals(chemicalsList);
        }
    }

    public Component check1() {
        return reactor.displayReactionPlan();
    }

    public Component check2() {
        return reactor.check();
    }

    public Component fillWithWater() {
        return reactor.testFill();
    }

    public Component fillTest() {
        return reactor.testFill2();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

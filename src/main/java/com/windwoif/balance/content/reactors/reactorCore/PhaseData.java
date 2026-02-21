package com.windwoif.balance.content.reactors.reactorCore;

import com.windwoif.balance.content.reactors.recipe.chemical.Chemical;
import net.minecraft.nbt.CompoundTag;

public record PhaseData(Chemical.State state, double volume, int color) {
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("State", state.ordinal());
        tag.putDouble("Volume", volume);
        tag.putInt("Color", color);
        return tag;
    }

    public static PhaseData fromNBT(CompoundTag tag) {
        Chemical.State state = Chemical.State.values()[tag.getInt("State")];
        double volume = tag.getDouble("Volume");
        int color = tag.getInt("Color");
        return new PhaseData(state, volume, color);
    }
}
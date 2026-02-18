package com.windwoif.balance.content.reactors.reactorCore;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.schematic.requirement.SpecialEntityItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.windwoif.balance.AllEntityTypes;
import com.windwoif.balance.AllItems;
import com.windwoif.balance.Chemical;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class ReactorEntity extends Entity implements IEntityAdditionalSpawnData, SpecialEntityItemRequirement {

    private Reactor reactor;

    public static AABB span(BlockPos startPos, BlockPos endPos) {
        return new AABB(startPos, endPos).expandTowards(1, 1, 1);
    }

    public ReactorEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public ReactorEntity(Level world, AABB boundingBox) {
        this(AllEntityTypes.REACTOR_ENTITY.get(), world);
        setBoundingBox(boundingBox);
        resetPositionToBB();
        reactor = new Reactor(getVolume(),298,10000);
    }

    public void resetPositionToBB() {
        AABB bb = getBoundingBox();
        setPosRaw(bb.getCenter().x, bb.minY, bb.getCenter().z);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        xRotO = getXRot();
        yRotO = getYRot();
        walkDistO = walkDist;
        xo = getX();
        yo = getY();
        zo = getZ();

        if (getBoundingBox().getXsize() == 0)
            discard();
        reactor.tick(0.05f);
    }

    public void addChemical(Chemical chemical, long amount) {
        if (reactor != null) reactor.changeChemical(chemical, amount);
    }

    public long getVolume() {
        AABB boundingBox = getBoundingBox();
        return (long) (boundingBox.getXsize() * boundingBox.getYsize() * boundingBox.getZsize()) * 1000;
    }

    public Reactor getReactor() {
        return reactor;
    }

    @Override
    public void setPos(double x, double y, double z) {
        AABB bb = getBoundingBox();
        setPosRaw(x, y, z);
        Vec3 center = bb.getCenter();
        setBoundingBox(bb.move(-center.x, -bb.minY, -center.z)
                .move(x, y, z));
    }

    @Override
    public void move(MoverType typeIn, Vec3 pos) {
        if (!level().isClientSide && isAlive() && pos.lengthSqr() > 0.0D)
            discard();
    }

    @Override
    public void push(double x, double y, double z) {
        if (!level().isClientSide && isAlive() && x * x + y * y + z * z > 0.0D)
            discard();
    }

    @Override
    protected float getEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
        return 0.0F;
    }

    public void playPlaceSound() {
        AllSoundEvents.SLIME_ADDED.playFrom(this, 0.5F, 0.75F);
    }

    @Override
    public void push(Entity entityIn) {
        super.push(entityIn);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        Vec3 position = position();
        writeBoundingBox(compound, getBoundingBox().move(position.scale(-1)));
        ListTag chemicalsList = reactor.SaveChemicals();
        compound.put("Chemicals", chemicalsList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        Vec3 position = position();
        setBoundingBox(readBoundingBox(compound).move(position));
        reactor = new Reactor(getVolume(),298,10000);
        if (compound.contains("Chemicals", Tag.TAG_LIST)) {
            ListTag chemicalsList = compound.getList("Chemicals", Tag.TAG_COMPOUND);
            reactor.LoadChemicals(chemicalsList);
        }
    }

    public static void writeBoundingBox(CompoundTag compound, AABB bb) {
        compound.put("From", VecHelper.writeNBT(new Vec3(bb.minX, bb.minY, bb.minZ)));
        compound.put("To", VecHelper.writeNBT(new Vec3(bb.maxX, bb.maxY, bb.maxZ)));
    }

    public static AABB readBoundingBox(CompoundTag compound) {
        Vec3 from = VecHelper.readNBT(compound.getList("From", Tag.TAG_DOUBLE));
        Vec3 to = VecHelper.readNBT(compound.getList("To", Tag.TAG_DOUBLE));
        return new AABB(from, to);
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public float rotate(Rotation transformRotation) {
        AABB bb = getBoundingBox().move(position().scale(-1));
        if (transformRotation == Rotation.CLOCKWISE_90 || transformRotation == Rotation.COUNTERCLOCKWISE_90)
            setBoundingBox(new AABB(bb.minZ, bb.minY, bb.minX, bb.maxZ, bb.maxY, bb.maxX).move(position()));
        return super.rotate(transformRotation);
    }

    @Override
    public float mirror(Mirror transformMirror) {
        return super.mirror(transformMirror);
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightningBolt) {}

    @Override
    public void refreshDimensions() {}

    public static EntityType.Builder<?> build(EntityType.Builder<?> builder) {
        @SuppressWarnings("unchecked")
        EntityType.Builder<ReactorEntity> entityBuilder = (EntityType.Builder<ReactorEntity>) builder;
        return entityBuilder;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        CompoundTag compound = new CompoundTag();
        addAdditionalSaveData(compound);
        buffer.writeNbt(compound);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        readAdditionalSaveData(additionalData.readNbt());
    }

    @Override
    public ItemRequirement getRequiredItems() {
        return new ItemRequirement(ItemUseType.DAMAGE, AllItems.WRENCH_ITEM.get());
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    public boolean contains(BlockPos pos) {
        return getBoundingBox().contains(Vec3.atCenterOf(pos));
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    public void setPortalEntrancePos() {
        portalEntrancePos = blockPosition();
    }

    @Override
    public PortalInfo findDimensionEntryPoint(ServerLevel pDestination) {
        return super.findDimensionEntryPoint(pDestination);
    }
}

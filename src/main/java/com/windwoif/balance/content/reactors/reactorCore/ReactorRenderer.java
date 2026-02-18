package com.windwoif.balance.content.reactors.reactorCore;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.windwoif.balance.Balance;
import com.windwoif.balance.Chemical;
import com.windwoif.balance.client.CubeRenderer;
import com.windwoif.balance.client.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ReactorRenderer extends EntityRenderer<ReactorEntity> {

	// Water animation: 32 frames, 2 ticks per frame
	private static final int WATER_FRAME_COUNT = 32;
	private static final int FRAME_TIME_TICKS = 2;

	private final TextureAtlasSprite waterSprite;
	private final TextureAtlasSprite gravelSprite;
	private final TextureAtlasSprite whiteConcreteSprite;
	private final TextureAtlasSprite lavaBWSprite;

	public ReactorRenderer(EntityRendererProvider.Context context) {
		super(context);
		var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
		this.waterSprite = atlas.getSprite(new ResourceLocation("balance", "block/water_still_opaque"));
		this.gravelSprite = atlas.getSprite(new ResourceLocation("balance", "block/gravel_white"));
		this.whiteConcreteSprite = atlas.getSprite(new ResourceLocation("balance", "block/white_concrete"));
		this.lavaBWSprite = atlas.getSprite(new ResourceLocation("balance", "block/lava_still_bw"));
	}

	@Override
	public void render(ReactorEntity entity, float entityYaw, float partialTick,
					   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		Reactor reactor = entity.getReactor();
		if (reactor == null) return;

		AABB bb = entity.getBoundingBox();
		float w = (float) bb.getXsize();
		float h = (float) bb.getYsize();
		float d = (float) bb.getZsize();

		// Move origin to the center of the bounding box
		poseStack.pushPose();
		poseStack.translate(bb.minX - entity.getX() + w/2,
				bb.minY - entity.getY() + h/2,
				bb.minZ - entity.getZ() + d/2);

		// Get the water sprite (animated)
		TextureAtlasSprite waterSprite = Minecraft.getInstance().getModelManager()
				.getAtlas(InventoryMenu.BLOCK_ATLAS)
				.getSprite(new ResourceLocation("balance", "block/water_still_opaque"));

		VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.TRANSLUCENT_NO_CULL);

        // Get sorted phases (density descending, bottom to top)
		List<Phase> phases = reactor.getSortedPhases();
		double totalVolume = reactor.getVolume(); // total capacity in liters
		double occupiedVolume = 0.0;

		for (Phase phase : phases) {
			double phaseVolume = phase.getVolume();
			if (phaseVolume <= 0.001) continue; // skip empty phases

			// Calculate vertical range for this phase
			double bottomY = -h/2 + (occupiedVolume / totalVolume) * h;
			double topY    = -h/2 + ((occupiedVolume + phaseVolume) / totalVolume) * h;
			float yMin = (float) bottomY;
			float yMax = (float) topY;

			// Get phase color (ARGB) – already includes concentration-based alpha
			int color = phase.getRenderColor();

			// Render the phase as a cube with water texture
			// We use the current frame offset to animate the water texture
			float vOffset = getWaterFrameOffset(entity, partialTick);
			// Note: CubeRenderer uses the sprite as-is; to incorporate animation,
			// we would need to modify renderFace to use vOffset, but for now we assume
			// the water sprite itself is animated (it is, because it's an animated texture).
			// If using a static sprite, you can pass a vOffset here.
			if (phase.getState() == Chemical.State.GAS) yMax = h/2;
			CubeRenderer.renderLiquidCube(poseStack, consumer,
					switch (phase.getState()){
                        case SOLID -> gravelSprite;
                        case GAS -> whiteConcreteSprite;
                        case MOLTEN_SALT -> lavaBWSprite;
                        default -> waterSprite;
                    },
					-w/2, yMin, -d/2,
					w/2, yMax,  d/2,
					color,
					switch (phase.getState()){
						case MOLTEN_SALT, MOLTEN_METAL -> 0xF000F0;
						default -> packedLight;
					});

			occupiedVolume += phaseVolume;
		}

		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	/**
	 * Calculate the vertical offset for the current animation frame.
	 * The result is a value between 0 and 1 that can be used to shift the V coordinate
	 * if the sprite is not automatically animated (e.g., when using a custom static texture).
	 */
	private float getWaterFrameOffset(ReactorEntity entity, float partialTick) {
		long gameTime = entity.level().getGameTime();
		long cycleLength = WATER_FRAME_COUNT * FRAME_TIME_TICKS;
		long frameIndex = (gameTime / FRAME_TIME_TICKS) % WATER_FRAME_COUNT;
		return (float) frameIndex / WATER_FRAME_COUNT;
	}

	@Override
	public ResourceLocation getTextureLocation(ReactorEntity entity) {
		return null; // Not used
	}
}
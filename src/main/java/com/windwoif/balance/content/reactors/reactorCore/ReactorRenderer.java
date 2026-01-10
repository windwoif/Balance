package com.windwoif.balance.content.reactors.reactorCore;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ReactorRenderer extends EntityRenderer<ReactorEntity> {

	public ReactorRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(ReactorEntity entity) {
		return null;
	}

	@Override
	public boolean shouldRender(ReactorEntity entity, Frustum frustum, double x, double y, double z) {
		return false;
	}

}

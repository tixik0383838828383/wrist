package com.timur.wristwatch.mixin;

import com.timur.wristwatch.client.render.WatchFeatureRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Как только игра создаёт рендерер модели игрока (обычный и slim/Alex вариант),
 * добавляем к нему наш FeatureRenderer с часами. Это работает и для локального игрока,
 * и для остальных игроков на сервере — ровно то, что нужно для мультиплеера.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void wristwatch$addWatchFeature(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
		@SuppressWarnings("unchecked")
		LivingEntityRendererAccessor<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> accessor =
				(LivingEntityRendererAccessor<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>) (Object) this;

		@SuppressWarnings("unchecked")
		FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> self =
				(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>) (Object) this;

		accessor.wristwatch$addFeature(new WatchFeatureRenderer(self));
	}
}

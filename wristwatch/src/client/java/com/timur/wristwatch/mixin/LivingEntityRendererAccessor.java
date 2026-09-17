package com.timur.wristwatch.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Даёт доступ к защищённому LivingEntityRenderer#addFeature, чтобы добавить свой
 * WatchFeatureRenderer к рендереру игрока без использования более новых/непроверенных
 * реестровых API, которых может не быть в используемой версии Fabric API.
 */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor<T extends LivingEntity, M extends EntityModel<T>> {
	@Invoker("addFeature")
	boolean wristwatch$addFeature(FeatureRenderer<T, M> feature);
}

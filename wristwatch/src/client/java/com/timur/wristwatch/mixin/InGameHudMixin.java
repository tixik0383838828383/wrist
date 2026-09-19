package com.timur.wristwatch.mixin;

import com.timur.wristwatch.client.WatchConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Позволяет полностью скрыть стандартный хотбар внизу экрана (настройка "Скрыть обычный хотбар"),
 * когда вместо него используется плавающая панель WristWatch.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

	@Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
	private void wristwatch$hideHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (WatchConfig.get().hideVanillaHotbar) {
			ci.cancel();
		}
	}
}

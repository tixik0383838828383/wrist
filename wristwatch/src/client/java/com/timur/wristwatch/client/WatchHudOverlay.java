package com.timur.wristwatch.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/**
 * Плавающая панель "часов" в собственном виде игрока — как плавающее окно в VR-очках:
 * не привязана к 3D-модели руки, просто аккуратно выезжает сбоку экрана, пока часы активны.
 * Никакой матричной геометрии/костей — чистый 2D HUD, поэтому не зависит ни от Figura,
 * ни от модели персонажа.
 */
public final class WatchHudOverlay {

	private WatchHudOverlay() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register(WatchHudOverlay::render);
	}

	private static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}

		float progress = ClientWatchState.getProgress(client.player.getUuid());
		if (progress <= 0.0f) {
			return;
		}

		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();

		int panelWidth = 190;
		int panelHeight = 42;

		// Панель висит справа по центру экрана и плавно выезжает сбоку по мере progress,
		// не перекрывая ни прицел, ни сам хотбар внизу.
		int targetX = screenWidth - panelWidth - 16;
		int startX = screenWidth + 10;
		int x = (int) (startX + (targetX - startX) * progress);
		int y = screenHeight / 2 - panelHeight / 2 - 40;

		int alpha = (int) (progress * 200);
		int bgColor = (alpha << 24);
		int borderColor = (alpha << 24) | 0x555560;

		context.fill(x, y, x + panelWidth, y + panelHeight, bgColor);
		context.fill(x, y, x + panelWidth, y + 1, borderColor);
		context.fill(x, y + panelHeight - 1, x + panelWidth, y + panelHeight, borderColor);

		PlayerInventory inventory = client.player.getInventory();
		int slots = 9;
		int iconSize = 18;
		int spacing = 20;
		int startXIcons = x + (panelWidth - slots * spacing) / 2;
		int iconY = y + (panelHeight - iconSize) / 2;

		for (int i = 0; i < slots; i++) {
			ItemStack stack = inventory.getStack(i);
			int iconX = startXIcons + i * spacing;

			if (i == inventory.selectedSlot) {
				context.fill(iconX - 1, iconY - 1, iconX + iconSize + 1, iconY + iconSize + 1, (alpha << 24) | 0xFFFFFF);
			}

			if (!stack.isEmpty()) {
				context.drawItem(stack, iconX, iconY);
				context.drawItemInSlot(client.textRenderer, stack, iconX, iconY);
			}
		}
	}
}

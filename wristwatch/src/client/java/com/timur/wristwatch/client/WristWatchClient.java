package com.timur.wristwatch.client;

import com.timur.wristwatch.WristWatchMod;
import com.timur.wristwatch.network.WatchStatePayload;
import com.timur.wristwatch.network.WatchSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class WristWatchClient implements ClientModInitializer {

	private static KeyBinding toggleWatchKey;
	private static KeyBinding openSettingsKey;
	private static boolean localActive = false;
	private static float localHeldSeconds = 0.0f;

	@Override
	public void onInitializeClient() {
		toggleWatchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.wristwatch.toggle",
				InputUtil.Type.KEYSYM,
				InputUtil.UNKNOWN_KEY.getCode(), // не забинжена по умолчанию — назначь клавишу в настройках управления
				"category.wristwatch"
		));

		openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.wristwatch.settings",
				InputUtil.Type.KEYSYM,
				InputUtil.UNKNOWN_KEY.getCode(), // тоже не забинжена по умолчанию
				"category.wristwatch"
		));

		// Получение состояния других игроков с сервера.
		ClientPlayNetworking.registerGlobalReceiver(WatchSyncPayload.ID, (payload, context) ->
				context.client().execute(() -> ClientWatchState.setActive(payload.playerId(), payload.active())));

		// При выходе с сервера/из мира чистим состояние, чтобы не тащить его в следующий мир.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientWatchState.clear();
			localActive = false;
			localHeldSeconds = 0.0f;
		});

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		// Плавающая панель часов у себя в виде (2D HUD, без 3D-геометрии — см. WatchHudOverlay).
		WatchHudOverlay.register();

		// Сам feature-рендерер добавляется к каждому PlayerEntityRenderer через
		// PlayerEntityRendererMixin (см. пакет mixin) — здесь ничего регистрировать не нужно.

		WristWatchMod.LOGGER.info("WristWatch: client init done");
	}

	private void onClientTick(MinecraftClient client) {
		if (client.player == null) {
			return;
		}

		if (client.currentScreen == null && openSettingsKey.wasPressed()) {
			client.setScreen(new WatchConfigScreen(null));
		}

		if (toggleWatchKey.wasPressed()) {
			setLocalActive(!localActive, client);
		}

		if (localActive) {
			localHeldSeconds += 1.0f / 20.0f; // клиентский тик = 1/20 сек
			if (localHeldSeconds >= WatchConfig.get().holdSeconds) {
				setLocalActive(false, client);
			}
		}

		// Продвигаем плавное появление/исчезание панели для всех видимых игроков ровно раз за тик,
		// чтобы feature-рендерер и HUD использовали одно и то же сглаженное значение.
		if (client.world != null) {
			for (var worldPlayer : client.world.getPlayers()) {
				ClientWatchState.tickProgress(worldPlayer.getUuid(), 1.0f / 20.0f);
			}
		}
	}

	private void setLocalActive(boolean active, MinecraftClient client) {
		localActive = active;
		localHeldSeconds = 0.0f;
		if (client.player != null) {
			ClientWatchState.setActive(client.player.getUuid(), active);
		}
		ClientPlayNetworking.send(new WatchStatePayload(active));
	}
}

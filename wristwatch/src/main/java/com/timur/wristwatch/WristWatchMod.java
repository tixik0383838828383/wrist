package com.timur.wristwatch;

import com.timur.wristwatch.network.WatchStatePayload;
import com.timur.wristwatch.network.WatchSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Общая (common) точка входа мода. Работает и на клиенте, и на сервере.
 * Здесь регистрируется сетевой протокол и серверная логика хранения/рассылки состояния "часов".
 */
public class WristWatchMod implements ModInitializer {

	public static final String MOD_ID = "wristwatch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Регистрируем типы пакетов (обязательно на обеих сторонах).
		PayloadTypeRegistry.playC2S().register(WatchStatePayload.ID, WatchStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(WatchSyncPayload.ID, WatchSyncPayload.CODEC);

		// Игрок сообщает серверу, что поднял/опустил "часы".
		ServerPlayNetworking.registerGlobalReceiver(WatchStatePayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			context.server().execute(() -> {
				ServerWatchManager.setActive(player, payload.active());
			});
		});

		// Когда новый игрок появляется в мире / начинает отслеживать других игроков,
		// присылаем ему текущее состояние всех активных "часов", чтобы не было рассинхрона.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerWatchManager.sendFullState(handler.getPlayer());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerWatchManager.onPlayerDisconnect(handler.getPlayer());
		});

		LOGGER.info("WristWatch: common init done");
	}
}

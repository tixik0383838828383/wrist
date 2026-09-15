package com.timur.wristwatch;

import com.timur.wristwatch.network.WatchSyncPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит на сервере, у каких игроков сейчас "поднята рука с часами", и рассылает
 * изменения всем остальным игрокам на сервере, чтобы третьи лица могли видеть анимацию/отрисовку.
 * Работает только для игроков, у которых установлен этот мод (клиент без мода просто не пришлёт пакет,
 * а другие клиенты без мода полученный пакет проигнорируют / не обработают, так как у них нет receiver'а).
 */
public final class ServerWatchManager {

	private static final Map<UUID, Boolean> ACTIVE_STATES = new ConcurrentHashMap<>();

	private ServerWatchManager() {
	}

	public static void setActive(ServerPlayerEntity player, boolean active) {
		UUID id = player.getUuid();
		Boolean previous = ACTIVE_STATES.get(id);
		if (previous != null && previous == active) {
			return; // ничего не изменилось, не спамим сеть
		}

		if (active) {
			ACTIVE_STATES.put(id, Boolean.TRUE);
		} else {
			ACTIVE_STATES.remove(id);
		}

		WatchSyncPayload payload = new WatchSyncPayload(id, active);
		// Рассылаем всем игрокам на сервере (включая самого игрока — не страшно, клиент идемпотентен).
		for (ServerPlayerEntity other : PlayerLookup.all(player.getServer())) {
			ServerPlayNetworking.send(other, payload);
		}
	}

	public static void onPlayerDisconnect(ServerPlayerEntity player) {
		UUID id = player.getUuid();
		if (ACTIVE_STATES.remove(id) != null) {
			WatchSyncPayload payload = new WatchSyncPayload(id, false);
			for (ServerPlayerEntity other : PlayerLookup.all(player.getServer())) {
				if (other != player) {
					ServerPlayNetworking.send(other, payload);
				}
			}
		}
	}

	/**
	 * Присылает вновь подключившемуся игроку полный список всех, у кого сейчас активны часы,
	 * чтобы у него сразу была правильная картина (без этого он бы узнал только о следующих переключениях).
	 */
	public static void sendFullState(ServerPlayerEntity newPlayer) {
		for (Map.Entry<UUID, Boolean> entry : ACTIVE_STATES.entrySet()) {
			if (entry.getValue()) {
				ServerPlayNetworking.send(newPlayer, new WatchSyncPayload(entry.getKey(), true));
			}
		}
	}
}

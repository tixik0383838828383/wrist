package com.timur.wristwatch.client;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиентское состояние: у кого из видимых игроков (включая нас самих) сейчас активны "часы",
 * и насколько далеко продвинулась анимация подъёма руки (0 = опущена, 1 = полностью поднята)
 * для плавной интерполяции у каждого игрока независимо.
 */
public final class ClientWatchState {

	private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();
	private static final Map<UUID, Float> PROGRESS = new ConcurrentHashMap<>();

	private ClientWatchState() {
	}

	public static void setActive(UUID playerId, boolean active) {
		if (active) {
			ACTIVE.put(playerId, Boolean.TRUE);
		} else {
			ACTIVE.remove(playerId);
		}
	}

	public static boolean isActive(PlayerEntity player) {
		return ACTIVE.getOrDefault(player.getUuid(), Boolean.FALSE);
	}

	public static boolean isActive(UUID playerId) {
		return ACTIVE.getOrDefault(playerId, Boolean.FALSE);
	}

	public static float getProgress(UUID playerId) {
		return PROGRESS.getOrDefault(playerId, 0.0f);
	}

	/**
	 * Продвигает анимацию для одного игрока на один тик/кадр в сторону target (0 или 1).
	 * Вызывается из feature-рендерера и из миксина на модель, чтобы оба использовали одно и то же значение.
	 */
	public static float tickProgress(UUID playerId, float deltaSeconds) {
		float target = isActive(playerId) ? 1.0f : 0.0f;
		float current = PROGRESS.getOrDefault(playerId, 0.0f);
		float speed = WatchConfig.get().animationSpeed;
		float step = speed * deltaSeconds;

		float next;
		if (current < target) {
			next = Math.min(target, current + step);
		} else if (current > target) {
			next = Math.max(target, current - step);
		} else {
			next = current;
		}

		if (next <= 0.0f && !isActive(playerId)) {
			PROGRESS.remove(playerId);
			return 0.0f;
		}

		PROGRESS.put(playerId, next);
		return next;
	}

	public static void clear() {
		ACTIVE.clear();
		PROGRESS.clear();
	}
}

package com.timur.wristwatch.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Сервер -> Клиенты: "у игрока с таким UUID часы включены/выключены".
 * Рассылается всем при изменении состояния и целиком присылается новому/переподключившемуся игроку.
 */
public record WatchSyncPayload(UUID playerId, boolean active) implements CustomPayload {
	public static final CustomPayload.Id<WatchSyncPayload> ID =
			new CustomPayload.Id<>(Identifier.of("wristwatch", "watch_sync"));

	public static final PacketCodec<RegistryByteBuf, WatchSyncPayload> CODEC = PacketCodec.of(
			(value, buf) -> {
				buf.writeUuid(value.playerId());
				buf.writeBoolean(value.active());
			},
			buf -> new WatchSyncPayload(buf.readUuid(), buf.readBoolean())
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

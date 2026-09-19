package com.timur.wristwatch.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Клиент -> Сервер: "я поднял(а) / опустил(а) руку с часами".
 */
public record WatchStatePayload(boolean active) implements CustomPayload {
	public static final CustomPayload.Id<WatchStatePayload> ID =
			new CustomPayload.Id<>(Identifier.of("wristwatch", "watch_state"));

	// Кодек написан вручную через writeBoolean/readBoolean, чтобы не зависеть от
	// точного названия готовой константы в PacketCodecs (оно отличается между версиями маппингов).
	public static final PacketCodec<RegistryByteBuf, WatchStatePayload> CODEC = PacketCodec.of(
			(value, buf) -> buf.writeBoolean(value.active()),
			buf -> new WatchStatePayload(buf.readBoolean())
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

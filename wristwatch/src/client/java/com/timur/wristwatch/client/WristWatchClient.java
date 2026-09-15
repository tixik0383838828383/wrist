package com.timur.wristwatch.client;

import com.timur.wristwatch.WristWatchMod;
import com.timur.wristwatch.network.WatchStatePayload;
import com.timur.wristwatch.network.WatchSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class WristWatchClient implements ClientModInitializer {

	private static KeyBinding toggleWatchKey;
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

		// Сам feature-рендерер добавляется к каждому PlayerEntityRenderer через
		// PlayerEntityRendererMixin (см. пакет mixin) — здесь ничего регистрировать не нужно.

		registerCommands();

		WristWatchMod.LOGGER.info("WristWatch: client init done");
	}

	private void onClientTick(MinecraftClient client) {
		if (client.player == null) {
			return;
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

		// Продвигаем анимацию подъёма/опускания руки для всех видимых игроков ровно раз за тик,
		// чтобы миксин на модель и feature-рендерер использовали одно и то же сглаженное значение.
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

	private void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(literal("wristwatch")
						.then(literal("arm")
								.then(argument("side", word())
										.executes(ctx -> {
											String side = getString(ctx, "side");
											WatchConfig cfg = WatchConfig.get();
											if (side.equalsIgnoreCase("left")) {
												cfg.arm = Arm.LEFT;
											} else if (side.equalsIgnoreCase("right")) {
												cfg.arm = Arm.RIGHT;
											} else {
												ctx.getSource().sendFeedback(Text.literal("Укажи left или right"));
												return 0;
											}
											cfg.save();
											ctx.getSource().sendFeedback(Text.literal("WristWatch: рука установлена — " + side));
											return 1;
										})))
						.then(literal("offset")
								.then(argument("x", floatArg())
										.then(argument("y", floatArg())
												.then(argument("z", floatArg())
														.executes(ctx -> {
															WatchConfig cfg = WatchConfig.get();
															cfg.offsetX = getFloat(ctx, "x");
															cfg.offsetY = getFloat(ctx, "y");
															cfg.offsetZ = getFloat(ctx, "z");
															cfg.save();
															ctx.getSource().sendFeedback(Text.literal("WristWatch: смещение обновлено"));
															return 1;
														})))))
						.then(literal("scale")
								.then(argument("value", floatArg(0.05f, 3.0f))
										.executes(ctx -> {
											WatchConfig cfg = WatchConfig.get();
											cfg.scale = getFloat(ctx, "value");
											cfg.save();
											ctx.getSource().sendFeedback(Text.literal("WristWatch: масштаб обновлён"));
											return 1;
										})))
						.then(literal("pose")
								.then(argument("pitch", floatArg())
										.then(argument("yaw", floatArg())
												.then(argument("roll", floatArg())
														.executes(ctx -> {
															WatchConfig cfg = WatchConfig.get();
															cfg.posePitchDeg = getFloat(ctx, "pitch");
															cfg.poseYawDeg = getFloat(ctx, "yaw");
															cfg.poseRollDeg = getFloat(ctx, "roll");
															cfg.save();
															ctx.getSource().sendFeedback(Text.literal("WristWatch: поза руки обновлена"));
															return 1;
														})))))
						.then(literal("duration")
								.then(argument("seconds", floatArg(0.5f, 60.0f))
										.executes(ctx -> {
											WatchConfig cfg = WatchConfig.get();
											cfg.holdSeconds = getFloat(ctx, "seconds");
											cfg.save();
											ctx.getSource().sendFeedback(Text.literal("WristWatch: время удержания обновлено"));
											return 1;
										})))
				));
	}
}

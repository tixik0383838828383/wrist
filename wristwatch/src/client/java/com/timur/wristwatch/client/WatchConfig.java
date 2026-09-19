package com.timur.wristwatch.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Настройки мода, которые можно менять прямо в игре через экран настроек
 * (клавиша "Open WristWatch settings"), без пересборки мода. Сохраняются в config/wristwatch.json.
 *
 * offset — смещение панели (которую видят ДРУГИЕ игроки) относительно лица игрока, чтобы она
 * висела перед лицом, а не сбоку/на макушке. Значения по умолчанию — отправная точка, почти
 * наверняка придётся чуть подвинуть на глаз через экран настроек.
 */
public class WatchConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("wristwatch.json");

	// Смещение панели (для сторонних наблюдателей) относительно головы, в блоках.
	// По умолчанию — прямо перед лицом, по направлению взгляда.
	public float offsetX = 0.0f;
	public float offsetY = 0.0f;
	public float offsetZ = 0.35f;

	// Масштаб панели у других игроков.
	public float scale = 0.6f;

	// Сколько секунд держится активная панель после нажатия клавиши, если не отпустить раньше.
	public float holdSeconds = 4.0f;

	// Скорость появления/исчезания панели (доля пути в секунду).
	public float animationSpeed = 6.0f;

	// Скрыть стандартный хотбар внизу экрана — вместо него используется плавающая панель WristWatch.
	public boolean hideVanillaHotbar = false;

	private static WatchConfig instance;

	public static WatchConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static WatchConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				WatchConfig loaded = GSON.fromJson(reader, WatchConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException | RuntimeException e) {
				com.timur.wristwatch.WristWatchMod.LOGGER.warn("WristWatch: не удалось прочитать конфиг, использую значения по умолчанию", e);
			}
		}
		WatchConfig fresh = new WatchConfig();
		fresh.save();
		return fresh;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			com.timur.wristwatch.WristWatchMod.LOGGER.warn("WristWatch: не удалось сохранить конфиг", e);
		}
	}
}

package com.timur.wristwatch.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Arm;

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
 * offset — смещение индикатора (для сторонних наблюдателей) относительно головы игрока;
 * pose — целевой угол, в который едет поднятая рука. Значения по умолчанию — отправная точка,
 * почти наверняка придётся чуть подвинуть на глаз через экран настроек.
 */
public class WatchConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("wristwatch.json");

	public Arm arm = Arm.LEFT;

	// Смещение "циферблата" относительно головы (крепится к голове — см. WatchFeatureRenderer),
	// в блоках (1.0 = целый блок). По умолчанию — сбоку от головы, как маленький значок.
	public float offsetX = 0.45f;
	public float offsetY = -0.1f;
	public float offsetZ = 0.0f;

	// Масштаб циферблата.
	public float scale = 0.6f;

	// Целевой угол поднятой руки в градусах (плечевой сустав), к которому едет анимация.
	public float posePitchDeg = -95.0f;
	public float poseYawDeg = 20.0f;
	public float poseRollDeg = 5.0f;

	// Сколько секунд держится поднятая рука после нажатия клавиши, если не отпустить раньше.
	public float holdSeconds = 4.0f;

	// Скорость подъёма/опускания руки (доля пути в секунду).
	public float animationSpeed = 6.0f;

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

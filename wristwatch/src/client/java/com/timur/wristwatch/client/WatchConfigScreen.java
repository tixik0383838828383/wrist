package com.timur.wristwatch.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

import java.util.function.DoubleSupplier;

/**
 * Экран настроек мода: Настройки -> (в игре) клавиша WristWatch: Settings.
 * Все значения применяются сразу и сохраняются в config/wristwatch.json при закрытии.
 *
 * Геттеры/сеттеры сделаны на примитивах (DoubleSupplier / свой FloatSetter), чтобы
 * не связываться с боксингом Double<->float в лямбдах вообще.
 */
public class WatchConfigScreen extends Screen {

	@FunctionalInterface
	private interface FloatSetter {
		void set(float value);
	}

	private final Screen parent;
	private final WatchConfig cfg;

	public WatchConfigScreen(Screen parent) {
		super(Text.literal("Настройки WristWatch"));
		this.parent = parent;
		this.cfg = WatchConfig.get();
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int y = this.height / 2 - 110;
		int rowHeight = 22;
		int fieldWidth = 220;

		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Рука: " + (cfg.arm == Arm.LEFT ? "левая" : "правая")),
				btn -> {
					cfg.arm = cfg.arm == Arm.LEFT ? Arm.RIGHT : Arm.LEFT;
					btn.setMessage(Text.literal("Рука: " + (cfg.arm == Arm.LEFT ? "левая" : "правая")));
				}
		).dimensions(centerX - fieldWidth / 2, y, fieldWidth, 20).build());
		y += rowHeight;

		y = addSlider(centerX, y, fieldWidth, "Смещение X", -2.0, 2.0,
				() -> cfg.offsetX, value -> cfg.offsetX = value);
		y = addSlider(centerX, y, fieldWidth, "Смещение Y", -2.0, 2.0,
				() -> cfg.offsetY, value -> cfg.offsetY = value);
		y = addSlider(centerX, y, fieldWidth, "Смещение Z", -2.0, 2.0,
				() -> cfg.offsetZ, value -> cfg.offsetZ = value);
		y = addSlider(centerX, y, fieldWidth, "Масштаб", 0.05, 3.0,
				() -> cfg.scale, value -> cfg.scale = value);
		y = addSlider(centerX, y, fieldWidth, "Угол руки: вверх/вниз", -180.0, 180.0,
				() -> cfg.posePitchDeg, value -> cfg.posePitchDeg = value);
		y = addSlider(centerX, y, fieldWidth, "Угол руки: влево/вправо", -180.0, 180.0,
				() -> cfg.poseYawDeg, value -> cfg.poseYawDeg = value);
		y = addSlider(centerX, y, fieldWidth, "Угол руки: поворот", -180.0, 180.0,
				() -> cfg.poseRollDeg, value -> cfg.poseRollDeg = value);
		y = addSlider(centerX, y, fieldWidth, "Скорость анимации", 0.5, 20.0,
				() -> cfg.animationSpeed, value -> cfg.animationSpeed = value);
		y = addSlider(centerX, y, fieldWidth, "Время удержания (сек)", 0.5, 20.0,
				() -> cfg.holdSeconds, value -> cfg.holdSeconds = value);

		y += 6;
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), btn -> {
			cfg.save();
			this.client.setScreen(parent);
		}).dimensions(centerX - fieldWidth / 2, y, fieldWidth, 20).build());
	}

	private int addSlider(int centerX, int y, int width, String label, double min, double max,
						   DoubleSupplier getter, FloatSetter setter) {
		double initial = MathHelper.clamp((getter.getAsDouble() - min) / (max - min), 0.0, 1.0);
		this.addDrawableChild(new LabeledSlider(centerX - width / 2, y, width, 20, label, min, max, initial, setter));
		return y + 22;
	}

	@Override
	public void close() {
		cfg.save();
		this.client.setScreen(parent);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/**
	 * Простой ползунок с подписью и текущим значением в тексте, без отдельного класса на каждый параметр.
	 */
	private static class LabeledSlider extends SliderWidget {
		private final String label;
		private final double min;
		private final double max;
		private final FloatSetter setter;

		LabeledSlider(int x, int y, int width, int height, String label, double min, double max,
					  double normalizedInitial, FloatSetter setter) {
			super(x, y, width, height, Text.literal(""), normalizedInitial);
			this.label = label;
			this.min = min;
			this.max = max;
			this.setter = setter;
			this.updateMessage();
		}

		private double currentValue() {
			return min + (max - min) * this.value;
		}

		@Override
		protected void updateMessage() {
			double v = currentValue();
			this.setMessage(Text.literal(label + ": " + String.format("%.2f", v)));
		}

		@Override
		protected void applyValue() {
			setter.set((float) currentValue());
		}
	}
}

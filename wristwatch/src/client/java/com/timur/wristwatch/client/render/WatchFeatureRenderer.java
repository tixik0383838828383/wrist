package com.timur.wristwatch.client.render;

import com.timur.wristwatch.client.ClientWatchState;
import com.timur.wristwatch.client.WatchConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * Рисует небольшую панель с содержимым хотбара ПЕРЕД ЛИЦОМ игрока — то, что видят ДРУГИЕ
 * игроки, когда кто-то включил часы (у себя игрок видит отдельную плавающую HUD-панель —
 * см. WatchHudOverlay). Активность и прогресс появления берутся из ClientWatchState, куда
 * попадают данные с сервера по сети. Никакой анимации руки больше нет — включение/выключение
 * просто плавно показывает/прячет эту панель.
 *
 * Крепится к ГОЛОВЕ (а не к руке): голова почти у всех моделей (в т.ч. кастомных Figura)
 * остаётся близко к ванильной позиции скелета. Смещение (offsetZ по умолчанию) уводит панель
 * вперёд по направлению взгляда, поэтому она следует за поворотом головы, как AR-дисплей.
 * Если у Figura-аватара рендер полностью заменён Lua-скриптами (как у Stick Figures),
 * визуальная модель может не совпадать с ванильным скелетом на 100% — тогда точное положение
 * панели подгоняется через смещения в настройках мода.
 */
public class WatchFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

	private static final int HOTBAR_SLOTS = 9;

	public WatchFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
		super(context);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
						AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
						float animationProgress, float headYaw, float headPitch) {

		UUID id = player.getUuid();
		float progress = ClientWatchState.getProgress(id);
		if (progress <= 0.0f) {
			return;
		}

		WatchConfig cfg = WatchConfig.get();
		ModelPart head = getContextModel().head;

		matrices.push();
		// Крепим к голове, а не к руке: голова почти у всех моделей (в т.ч. кастомных Figura)
		// остаётся близко к ванильной позиции, в отличие от кисти/предплечья, которые могут
		// быть сильно переопределены кастомной моделью и никак не совпадать с ванильным rig'ом.
		head.rotate(matrices);
		matrices.translate(cfg.offsetX, cfg.offsetY, cfg.offsetZ);

		float scale = cfg.scale * progress;
		matrices.scale(scale, scale, scale);

		renderPlate(matrices, vertexConsumers);
		renderHotbarIcons(matrices, vertexConsumers, light, player);

		matrices.pop();
	}

	private void renderPlate(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
		VertexConsumer plate = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
		Matrix4f matrix = matrices.peek().getPositionMatrix();

		float halfW = 0.85f;
		float halfH = 0.26f;

		// Тёмная подложка циферблата.
		quad(plate, matrix, halfW, halfH, 20, 20, 24, 220);
	}

	private void quad(VertexConsumer consumer, Matrix4f matrix, float halfW, float halfH, int r, int g, int b, int a) {
		consumer.vertex(matrix, -halfW, halfH, 0).color(r, g, b, a);
		consumer.vertex(matrix, halfW, halfH, 0).color(r, g, b, a);
		consumer.vertex(matrix, halfW, -halfH, 0).color(r, g, b, a);
		consumer.vertex(matrix, -halfW, -halfH, 0).color(r, g, b, a);
	}

	private void renderHotbarIcons(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player) {
		PlayerInventory inventory = player.getInventory();
		ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

		float iconScale = 0.17f;
		float spacing = 0.175f;
		float startX = -((HOTBAR_SLOTS - 1) * spacing) / 2.0f;

		for (int i = 0; i < HOTBAR_SLOTS; i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isEmpty()) {
				continue;
			}

			matrices.push();
			matrices.translate(startX + i * spacing, 0.0, 0.02);
			matrices.scale(iconScale, iconScale, iconScale);
			// renderItem рисует предмет в плоскости GUI лицом к "экрану" по умолчанию в другую сторону —
			// разворачиваем его на 180°, чтобы иконка смотрела наружу от руки, а не внутрь.
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));

			itemRenderer.renderItem(stack, ModelTransformationMode.GUI, light, OverlayTexture.DEFAULT_UV,
					matrices, vertexConsumers, player.getWorld(), 0);

			matrices.pop();

			if (i == inventory.selectedSlot) {
				matrices.push();
				matrices.translate(startX + i * spacing, 0.0, 0.019);
				matrices.scale(iconScale * 1.35f, iconScale * 1.35f, 1.0f);
				VertexConsumer highlight = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
				Matrix4f matrix = matrices.peek().getPositionMatrix();
				quad(highlight, matrix, 0.5f, 0.5f, 255, 255, 255, 90);
				matrices.pop();
			}
		}
	}
}

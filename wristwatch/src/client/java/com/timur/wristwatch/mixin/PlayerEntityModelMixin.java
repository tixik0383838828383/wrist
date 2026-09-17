package com.timur.wristwatch.mixin;

import com.timur.wristwatch.client.ClientWatchState;
import com.timur.wristwatch.client.WatchConfig;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * После того как игра посчитала обычную позу игрока (ходьба, атака, использование предмета и т.д.),
 * дополнительно "догибаем" выбранную руку в сторону позы "смотрю на часы" — пропорционально
 * прогрессу анимации (0 = рука опущена как обычно, 1 = рука полностью поднята к лицу).
 */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin<T extends LivingEntity> extends BipedEntityModel<T> {

	@Shadow
	public ModelPart leftSleeve;

	@Shadow
	public ModelPart rightSleeve;

	public PlayerEntityModelMixin(ModelPart root) {
		super(root);
	}

	@Inject(method = "setAngles", at = @At("TAIL"))
	private void wristwatch$applyWatchPose(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof PlayerEntity)) {
			return;
		}

		float progress = ClientWatchState.getProgress(entity.getUuid());
		if (progress <= 0.0f) {
			return;
		}

		WatchConfig cfg = WatchConfig.get();
		boolean isLeft = cfg.arm == Arm.LEFT;
		ModelPart arm = isLeft ? this.leftArm : this.rightArm;
		ModelPart sleeve = isLeft ? this.leftSleeve : this.rightSleeve;

		float targetPitch = cfg.posePitchDeg * MathHelper.RADIANS_PER_DEGREE;
		float targetYaw = (isLeft ? -1 : 1) * cfg.poseYawDeg * MathHelper.RADIANS_PER_DEGREE;
		float targetRoll = (isLeft ? -1 : 1) * cfg.poseRollDeg * MathHelper.RADIANS_PER_DEGREE;

		arm.pitch = MathHelper.lerp(progress, arm.pitch, targetPitch);
		arm.yaw = MathHelper.lerp(progress, arm.yaw, targetYaw);
		arm.roll = MathHelper.lerp(progress, arm.roll, targetRoll);

		if (sleeve != null) {
			sleeve.pitch = arm.pitch;
			sleeve.yaw = arm.yaw;
			sleeve.roll = arm.roll;
		}
	}
}

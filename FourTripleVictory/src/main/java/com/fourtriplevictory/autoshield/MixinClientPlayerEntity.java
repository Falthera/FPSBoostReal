package com.fourtriplevictory.autoshield;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!FourTripleVictoryClient.hitboxClipEnabled || target == null || !(target instanceof LivingEntity livingTarget)) {
            return;
        }
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        double maxReach = MinecraftClient.getInstance().interactionManager.getCurrentGameMode() == GameMode.CREATIVE ? 6.0 : 3.0;
        if (self.getEyePos().squaredDistanceTo(livingTarget.getEyePos()) > maxReach * maxReach * 0.9) {
            return;
        }
        double dx = livingTarget.getX() - self.getX();
        double dy = (livingTarget.getY() + livingTarget.getHeight() / 2.0) - self.getEyePos().y;
        double dz = livingTarget.getZ() - self.getZ();
        float targetYaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float targetPitch = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
        float deltaYaw = targetYaw - self.getYaw();
        while (deltaYaw > 180) deltaYaw -= 360;
        while (deltaYaw < -180) deltaYaw += 360;
        float offset = (float) (Math.random() * 0.03 - 0.01);
        self.setYaw(self.getYaw() + deltaYaw * offset);
        self.setPitch(self.getPitch() + (targetPitch - self.getPitch()) * offset);
    }
}

package com.fourtriplevictory.autoshield;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @Unique
    private boolean reduceKnockback = false;

    @Inject(method = "method_6099", at = @At("HEAD"), cancellable = true)
    private void onDamage(CallbackInfoReturnable<Boolean> cir) {
        if (!FourTripleVictoryClient.knockbackReductionEnabled) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ClientPlayerEntity)) {
            return;
        }
        reduceKnockback = true;
    }

    @Inject(method = "method_6099", at = @At("TAIL"))
    private void onDamageTail(CallbackInfoReturnable<Boolean> cir) {
        if (!reduceKnockback) {
            return;
        }
        reduceKnockback = false;
        LivingEntity self = (LivingEntity) (Object) this;
        Vec3d vel = self.getVelocity();
        self.setVelocity(vel.x * 0.92, vel.y, vel.z * 0.92);
    }
}

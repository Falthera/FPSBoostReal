package com.fourtriplevictory.autoshield.mixin;

import com.fourtriplevictory.autoshield.JumpResetController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.ModifyArg;
import org.spongepowered.asm.mixin.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "method_6099", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            JumpResetController.onLocalPlayerDamaged((ClientPlayerEntity) (Object) this, source, amount);
        }
    }

    @ModifyArg(method = "method_6099", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;knockback(DD)V"), index = 0)
    private double modifyKnockback(double strength) {
        return strength * 0.92;
    }
}

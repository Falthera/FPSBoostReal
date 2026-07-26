package com.fourtriplevictory.autoshield;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
    @Unique
    private static double microReach = 0.08;

    @Inject(method = "method_2903", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!FourTripleVictoryClient.microReachEnabled || target == null) {
            return;
        }
        GameMode mode = MinecraftClient.getInstance().interactionManager.getCurrentGameMode();
        double baseReach = mode == GameMode.CREATIVE ? 6.0 : 3.0;
        double extendedReach = baseReach + microReach;
        if (MinecraftClient.getInstance().player.getEyePos().squaredDistanceTo(target.getEyePos()) > extendedReach * extendedReach) {
            return;
        }
    }
}

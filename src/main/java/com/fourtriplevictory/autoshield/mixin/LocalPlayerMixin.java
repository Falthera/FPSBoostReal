package com.fourtriplevictory.autoshield.mixin;

import com.fourtriplevictory.autoshield.FourTripleVictoryClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class LocalPlayerMixin {
    @Inject(method = "getEntityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void onGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (FourTripleVictoryClient.microReachEnabled && cir.getReturnValue() == 3.0) {
            cir.setReturnValue(3.08);
        }
    }
}

package com.fourtriplevictory.autoshield;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.damage.DamageSource;

public class JumpResetController {
    private static final int COOLDOWN_TICKS = 10;
    private static final int JUMP_DELAY_TICKS = 0;
    private static boolean cooldownActive = false;
    private static int cooldownTick = 0;
    private static boolean jumpPending = false;
    private static int scheduledJumpTick = 0;

    public static void onLocalPlayerDamaged(ClientPlayerEntity player, DamageSource source, float amount) {
        if (cooldownActive) return;
        if (!shouldTriggerJump(source)) return;

        cooldownActive = true;
        cooldownTick = COOLDOWN_TICKS;
        jumpPending = true;
        scheduledJumpTick = getCurrentTick() + JUMP_DELAY_TICKS;
    }

    private static boolean shouldTriggerJump(DamageSource source) {
        return source.getAttacker() != null;
    }

    public static void onEndTick() {
        if (cooldownActive) {
            cooldownTick--;
            if (cooldownTick <= 0) cooldownActive = false;
        }

        if (jumpPending && getCurrentTick() >= scheduledJumpTick) {
            if (JumpExecutor.canJump()) {
                MinecraftClient.getInstance().player.jump();
                jumpPending = false;
            }
        }
    }

    public static void onDisconnect() {
        cooldownActive = false;
        jumpPending = false;
    }

    private static int getCurrentTick() {
        return MinecraftClient.getInstance().player.age;
    }
}

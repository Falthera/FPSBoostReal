package com.fourtriplevictory.autoshield;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class JumpExecutor {
    public static boolean canJump() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        if (MinecraftClient.getInstance().currentScreen != null) return false;
        if (!player.isOnGround()) return false;
        if (player.isTouchingWater()) return false;
        if (player.isInLava()) return false;
        if (player.isClimbing()) return false;
        if (player.isFallFlying()) return false;
        return true;
    }
}

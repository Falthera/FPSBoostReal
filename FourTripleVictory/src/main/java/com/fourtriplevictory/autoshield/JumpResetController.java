package com.fourtriplevictory.autoshield;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ClientboundEntityEventPacket;
import net.minecraft.network.packet.s2c.play.ClientboundSetEntityMotionPacket;

public class JumpResetController {
    public static boolean ignoreNextVelocity = false;
    public static long lastDamageTime = 0;
    public static final int VELOCITY_IGNORE_WINDOW = 50;

    public static void onPacketReceived(Packet<?> packet) {
        if (packet instanceof ClientboundEntityEventPacket entityEvent) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && entityEvent.getEntityId() == player.getId()) {
                if (entityEvent.getEvent() == 2) {
                    handleDamageEvent();
                }
            }
        }
        if (packet instanceof ClientboundSetEntityMotionPacket velocityPacket) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && ignoreNextVelocity && System.currentTimeMillis() - lastDamageTime < VELOCITY_IGNORE_WINDOW) {
                if (velocityPacket.getId() == player.getId()) {
                    ignoreNextVelocity = false;
                }
            }
        }
    }

    private static void handleDamageEvent() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || !player.isOnGround()) {
            return;
        }
        if (Math.random() > FourTripleVictoryClient.jumpResetAccuracy) {
            return;
        }
        ignoreNextVelocity = true;
        lastDamageTime = System.currentTimeMillis();
        int delay = (int)(Math.random() * 50);
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                MinecraftClient.getInstance().execute(() -> {
                    if (player.isOnGround()) {
                        player.jump();
                    }
                });
            } catch (InterruptedException e) {
            }
        }).start();
    }
}

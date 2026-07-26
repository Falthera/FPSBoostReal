package com.fourtriplevictory.autoshield;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import java.lang.reflect.Field;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class FourTripleVictoryClient implements ClientModInitializer {
    public static boolean enabled = true;
    public static KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.fourtriplevictory.toggle",
                    GLFW.GLFW_KEY_R,
                    KeyBinding.Category.create(Identifier.of("key.categories.fourtriplevictory"))
            )
    );
    public static int originalSlot = -1;
    public static int swapBackTicks = -1;
    public static boolean needsSwapBack = false;
    public static boolean triggerbotEnabled = false;
    public static KeyBinding TRIGGERBOT_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.fourtriplevictory.triggerbot",
                    GLFW.GLFW_KEY_T,
                    KeyBinding.Category.create(Identifier.of("key.categories.fourtriplevictory"))
            )
    );
    public static long lastTriggerbotAttack = 0L;
    public static int triggerbotDelay = 50;
    public static int triggerbotRandomization = 20;
    public static boolean triggerbotPlayersOnly = true;
    public static double triggerbotRange = 3.5;

    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> {
            if (!enabled || world.isClient() && entity == null) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof PlayerEntity target)) {
                return ActionResult.PASS;
            }
            if (!target.isBlocking()) {
                return ActionResult.PASS;
            }
            PlayerInventory inv = player.getInventory();
            int axeSlot = findAxeSlot(inv);
            if (axeSlot == -1) {
                return ActionResult.PASS;
            }
            if (needsSwapBack) {
                try {
                    Field f = PlayerInventory.class.getDeclaredField("selectedSlot");
                    f.setAccessible(true);
                    originalSlot = f.getInt(inv);
                } catch (Exception e) {
                    return ActionResult.PASS;
                }
            }
            try {
                Field f = PlayerInventory.class.getDeclaredField("selectedSlot");
                f.setAccessible(true);
                f.setInt(inv, axeSlot);
            } catch (Exception e) {
                return ActionResult.PASS;
            }
            needsSwapBack = true;
            swapBackTicks = 5;
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (needsSwapBack) {
                swapBackTicks--;
                if (swapBackTicks <= 0) {
                     try {
                         Field f = PlayerInventory.class.getDeclaredField("selectedSlot");
                         f.setAccessible(true);
                         f.setInt(MinecraftClient.getInstance().player.getInventory(), originalSlot);
                     } catch (Exception e) {
                     }
                    needsSwapBack = false;
                    swapBackTicks = -1;
                }
            }
            if (TOGGLE_KEY.wasPressed()) {
                enabled = !enabled;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    if (enabled) {
                        mc.player.sendMessage(Text.literal("FPS Boost: ON").copy().withColor(0x55FF55), false);
                    } else {
                        mc.player.sendMessage(Text.literal("FPS Boost: OFF").copy().withColor(0xFF5555), false);
                    }
                }
            }
            if (TRIGGERBOT_KEY.wasPressed()) {
                triggerbotEnabled = !triggerbotEnabled;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    if (triggerbotEnabled) {
                        mc.player.sendMessage(Text.literal("Triggerbot: ON").copy().withColor(0x55FF55), false);
                    } else {
                        mc.player.sendMessage(Text.literal("Triggerbot: OFF").copy().withColor(0xFF5555), false);
                    }
                }
            }
            if (!triggerbotEnabled) {
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.player.isDead() || mc.player.isSpectator()) {
                return;
            }
            Entity target = mc.targetedEntity;
            if (target == null) {
                return;
            }
            if (triggerbotPlayersOnly && !(target instanceof PlayerEntity)) {
                return;
            }
            if (target.isDead() || target.isInvulnerable()) {
                return;
            }
            double distSq = mc.player.squaredDistanceTo(target);
            if (distSq > triggerbotRange * triggerbotRange) {
                return;
            }
            if (mc.player.getAttackCooldownProgress(0.0f) < 1.0f) {
                return;
            }
            if (mc.player.handSwinging) {
                return;
            }
            long now = System.currentTimeMillis();
            long elapsed = now - lastTriggerbotAttack;
            int jitter = (int) ((Math.random() * triggerbotRandomization * 2) - triggerbotRandomization);
            int adjustedDelay = Math.max(0, triggerbotDelay + jitter);
            if (elapsed < adjustedDelay) {
                return;
            }
            final Entity attackTarget = target;
            MinecraftClient.execute(() -> {
                if (mc.player != null && attackTarget != null && !attackTarget.isDead()) {
                    mc.player.attack(attackTarget);
                }
            });
            lastTriggerbotAttack = now;
        });
    }

    private int findAxeSlot(PlayerInventory inv) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }
}

package com.fourtriplevictory.autoshield;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
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
                    "key.categories.fourtriplevictory"
            )
    );
    public static int originalSlot = -1;
    public static int swapBackTicks = -1;
    public static boolean needsSwapBack = false;

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
                originalSlot = inv.selectedSlot;
            }
            inv.selectedSlot = axeSlot;
            needsSwapBack = true;
            swapBackTicks = 5;
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (needsSwapBack) {
                swapBackTicks--;
                if (swapBackTicks <= 0) {
                    MinecraftClient.getInstance().player.getInventory().selectedSlot = originalSlot;
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

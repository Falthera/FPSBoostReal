package com.fourtriplevictory.autoshield;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Items;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientConnection;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ClientboundEntityEventPacket;
import net.minecraft.network.packet.s2c.play.ClientboundSetEntityMotionPacket;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import com.mojang.brigadier.arguments.StringArgumentType;

public class FourTripleVictoryClient implements ClientModInitializer {
    public static boolean shieldBreakerEnabled = true;
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
    public static boolean webDrainEnabled = false;
    public static KeyBinding WEBDRAIN_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.fourtriplevictory.webdrain",
                    GLFW.GLFW_KEY_Y,
                    KeyBinding.Category.create(Identifier.of("key.categories.fourtriplevictory"))
            )
    );
    public static int webDrainRadius = 5;
    public static int webDrainDelay = 0;
    public static boolean webDrainHotbarOnly = true;
    public static float aimSpeed = 0.3f;
    public static float aimRandomization = 2.0f;
    public static int reactionTimeMin = 50;
    public static int reactionTimeMax = 150;
    public static float missChance = 0.05f;
    public static boolean isAiming = false;
    public static BlockPos aimTarget = null;
    public static long aimStartTime = 0L;
    public static float aimTargetYaw = 0f;
    public static float aimTargetPitch = 0f;

    public static boolean hitboxClipEnabled = true;
    public static boolean knockbackReductionEnabled = true;
    public static boolean microReachEnabled = true;
    public static boolean hitRegPriorityEnabled = true;
    public static boolean jumpResetEnabled = true;
    public static float jumpResetAccuracy = 0.85f;

    public static boolean ignoreNextVelocity = false;
    public static long lastDamageTime = 0L;
    public static final int VELOCITY_IGNORE_WINDOW = 50;

    public static boolean attackQueued = false;
    public static Entity queuedTarget = null;
    public static final java.util.Set<String> trustedPlayers = new java.util.HashSet<>();

    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> {
            if (!shieldBreakerEnabled || entity == null) {
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
            if (!needsSwapBack) {
                originalSlot = inv.getSelectedSlot();
            }
            inv.setSelectedSlot(axeSlot);
            needsSwapBack = true;
            swapBackTicks = 5;
            return ActionResult.PASS;
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (attackQueued && queuedTarget != null && MinecraftClient.getInstance().player != null && !((LivingEntity) queuedTarget).isDead()) {
                MinecraftClient.getInstance().player.attack(queuedTarget);
                attackQueued = false;
                queuedTarget = null;
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return;
            }
            if (needsSwapBack) {
                swapBackTicks--;
                if (swapBackTicks <= 0) {
                    mc.player.getInventory().setSelectedSlot(originalSlot);
                    needsSwapBack = false;
                    swapBackTicks = -1;
                }
            }
            if (TOGGLE_KEY.wasPressed()) {
                shieldBreakerEnabled = !shieldBreakerEnabled;
                mc.player.sendMessage(Text.literal("Shield Breaker: " + (shieldBreakerEnabled ? "ON" : "OFF")).copy().withColor(shieldBreakerEnabled ? 0x55FF55 : 0xFF5555), false);
            }
            if (TRIGGERBOT_KEY.wasPressed()) {
                triggerbotEnabled = !triggerbotEnabled;
                mc.player.sendMessage(Text.literal("Triggerbot: " + (triggerbotEnabled ? "ON" : "OFF")).copy().withColor(triggerbotEnabled ? 0x55FF55 : 0xFF5555), false);
            }
            if (WEBDRAIN_KEY.wasPressed()) {
                webDrainEnabled = !webDrainEnabled;
                mc.player.sendMessage(Text.literal("Web Drain: " + (webDrainEnabled ? "ON" : "OFF")).copy().withColor(webDrainEnabled ? 0x55FF55 : 0xFF5555), false);
            }
            if (mc.player.isDead() || mc.player.isSpectator()) {
                return;
            }
            if (isAiming) {
                handleSmoothAim(mc);
                return;
            }
            if (triggerbotEnabled) {
                handleTriggerbot(mc);
            }
            if (webDrainEnabled) {
                handleWebDrain(mc);
            }
            if (attackQueued) {
                attackQueued = false;
            }
        });

        ClientConnection conn = MinecraftClient.getInstance().getNetworkHandler().getConnection();
        if (conn != null) {
            conn.channel.pipeline().addFirst(new ChannelInboundHandlerAdapter() {
                @Override
                public boolean acceptInboundMessage(Object msg) {
                    JumpResetController.onPacketReceived((Packet<?>) msg);
                    return true;
                }
            });
        }

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("triggerbot")
                .then(ClientCommandManager.literal("trust")
                    .then(ClientCommandManager.argument("player", StringArgumentType.string())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "player");
                            trustedPlayers.add(name.toLowerCase());
                            MinecraftClient.getInstance().player.sendMessage(Text.literal("Triggerbot trusted: " + name), false);
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.literal("untrust")
                    .then(ClientCommandManager.argument("player", StringArgumentType.string())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "player");
                            trustedPlayers.remove(name.toLowerCase());
                            MinecraftClient.getInstance().player.sendMessage(Text.literal("Triggerbot untrusted: " + name), false);
                            return 1;
                        })
                    )
                )
            );
        });
    }

    private void handleTriggerbot(MinecraftClient mc) {
        Entity target = mc.targetedEntity;
        if (target == null) {
            return;
        }
        if (target instanceof PlayerEntity playerTarget && trustedPlayers.contains(playerTarget.getName().getString().toLowerCase())) {
            return;
        }
        if (triggerbotPlayersOnly && !(target instanceof PlayerEntity)) {
            return;
        }
        if (((LivingEntity) target).isDead() || target.isInvulnerable()) {
            return;
        }
        double distSq = mc.player.getEyePos().squaredDistanceTo(target.getEyePos());
        double reachSq = triggerbotRange * triggerbotRange;
        if (mc.interactionManager != null && mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
            reachSq = 36.0;
        }
        if (distSq > reachSq) {
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
        if (hitRegPriorityEnabled) {
            attackQueued = true;
            queuedTarget = target;
        } else {
            mc.execute(() -> {
                if (mc.player != null && target != null && !((LivingEntity) target).isDead()) {
                    mc.player.attack(target);
                }
            });
        }
        lastTriggerbotAttack = now;
    }

    private void handleWebDrain(MinecraftClient mc) {
        if (isAiming) {
            return;
        }
        PlayerEntity player = mc.player;
        World world = mc.world;
        if (world == null || player == null) {
            return;
        }
        Vec3d eyePos = player.getEyePos();
        double reachSq = 9.0;
        if (mc.interactionManager != null && mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
            reachSq = 36.0;
        }
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        double bestDist = Double.MAX_VALUE;
        BlockPos bestPos = null;
        int radius = webDrainRadius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    mutable.set(player.getBlockX() + x, player.getBlockY() + y, player.getBlockZ() + z);
                    BlockState state = world.getBlockState(mutable);
                    if (state.getBlock() != Blocks.WATER || !state.getFluidState().isStill()) {
                        continue;
                    }
                    boolean adjacentToCobweb = false;
                    for (Direction dir : Direction.values()) {
                        mutable.move(dir);
                        if (world.getBlockState(mutable).getBlock() == Blocks.COBWEB) {
                            adjacentToCobweb = true;
                        }
                        mutable.move(dir.getOpposite());
                    }
                    if (!adjacentToCobweb) {
                        continue;
                    }
                    double distSq = eyePos.squaredDistanceTo(mutable.toCenterPos());
                    if (distSq > reachSq) {
                        continue;
                    }
                    if (distSq < bestDist) {
                        bestDist = distSq;
                        bestPos = mutable.toImmutable();
                    }
                }
            }
        }
        if (bestPos == null) {
            return;
        }
        startAiming(player, bestPos);
    }

    private void startAiming(PlayerEntity player, BlockPos pos) {
        isAiming = true;
        aimTarget = pos;
        Vec3d eyePos = player.getEyePos();
        Vec3d targetPos = Vec3d.ofCenter(pos);
        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        aimTargetYaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        aimTargetPitch = (float) Math.toDegrees(Math.atan2(-dy, horizontal));
        aimTargetYaw += (float) ((Math.random() - 0.5) * 2 * aimRandomization);
        aimTargetPitch += (float) ((Math.random() - 0.5) * 2 * aimRandomization);
        if (aimTargetPitch > 90) {
            aimTargetPitch = 90;
        } else if (aimTargetPitch < -90) {
            aimTargetPitch = -90;
        }
        aimStartTime = System.currentTimeMillis();
    }

    private void handleSmoothAim(MinecraftClient mc) {
        PlayerEntity player = mc.player;
        World world = mc.world;
        if (world == null || player == null || player.isDead()) {
            isAiming = false;
            aimTarget = null;
            return;
        }
        if (Math.random() < missChance) {
            isAiming = false;
            aimTarget = null;
            return;
        }
        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();
        float deltaYaw = aimTargetYaw - currentYaw;
        while (deltaYaw > 180) {
            deltaYaw -= 360;
        }
        while (deltaYaw < -180) {
            deltaYaw += 360;
        }
        float newYaw = currentYaw + deltaYaw * aimSpeed;
        float newPitch = currentPitch + (aimTargetPitch - currentPitch) * aimSpeed;
        player.setYaw(newYaw);
        player.setPitch(newPitch);
        boolean closeEnough = Math.abs(deltaYaw) < 5 && Math.abs(aimTargetPitch - currentPitch) < 5;
        boolean reactionPassed = System.currentTimeMillis() - aimStartTime >= reactionTimeMin + (long) (Math.random() * (reactionTimeMax - reactionTimeMin));
        if (closeEnough && reactionPassed) {
            placeCobweb(mc, player, world);
        }
    }

    private void placeCobweb(MinecraftClient mc, PlayerEntity player, World world) {
        int originalSlot = player.getInventory().getSelectedSlot();
        int cobwebSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.COBWEB) {
                cobwebSlot = i;
                break;
            }
        }
        if (cobwebSlot == -1 && !webDrainHotbarOnly) {
            for (int i = 9; i < 36; i++) {
                if (player.getInventory().getStack(i).getItem() == Items.COBWEB) {
                    cobwebSlot = i;
                    break;
                }
            }
        }
        if (cobwebSlot == -1) {
            isAiming = false;
            aimTarget = null;
            return;
        }
        if (cobwebSlot != originalSlot) {
            player.getInventory().setSelectedSlot(cobwebSlot);
        }
        BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(aimTarget), Direction.UP, aimTarget, false);
        mc.interactionManager.interactBlock((ClientPlayerEntity) player, Hand.MAIN_HAND, hitResult);
        if (cobwebSlot != originalSlot) {
            player.getInventory().setSelectedSlot(originalSlot);
        }
        isAiming = false;
        aimTarget = null;
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

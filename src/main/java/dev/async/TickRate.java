package dev.async;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class TickRate {

    private final AsyncConfig config;

    private boolean skipNextAttack;
    private boolean criticalAttackPerformed;

    // Mace state: one smash per fall, re-armed after the player goes up again
    // (wind charge / Wind Burst) or touches the ground.
    private static final double SMASH_FALL_DISTANCE = 1.5;
    private static final int MACE_COOLDOWN_TICKS = 4;

    private double maceApexY;
    private boolean maceHitThisFall;
    private int maceCooldown;

    public TickRate(AsyncConfig config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public TargetMode getMode() {
        return config.getTargetMode();
    }

    public TickMode getAttackMode() {
        return config.getAttackMode();
    }

    public TickWeapon getWeaponMode() {
        return config.getWeaponMode();
    }

    public boolean isKeyToggleEnabled() {
        return config.isKeyToggleEnabled();
    }

    public int getToggleKey() {
        return config.getToggleKey();
    }

    public void toggleEnabled() {
        config.setEnabled(!config.isEnabled());
        criticalAttackPerformed = false;
    }

    public void toggleTargetMode() {
        config.setTargetMode(config.getTargetMode().next());
    }

    public void toggleAttackMode() {
        config.setAttackMode(config.getAttackMode().next());
        criticalAttackPerformed = false;
    }

    public void toggleWeaponMode() {
        config.setWeaponMode(config.getWeaponMode().next());
    }

    public void toggleKeyToggleEnabled() {
        config.setKeyToggleEnabled(!config.isKeyToggleEnabled());
    }

    public void setToggleKey(int key) {
        config.setToggleKey(key);
    }

    public void suppressNextAttack() {
        skipNextAttack = true;
    }

    public void onEndTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;

        if (client.world == null || player == null) {
            skipNextAttack = false;
            criticalAttackPerformed = false;
            maceHitThisFall = false;
            maceCooldown = 0;
            return;
        }

        if (player.isOnGround()) {
            criticalAttackPerformed = false;
        }

        trackMaceFall(player);

        if (client.currentScreen != null) {
            skipNextAttack = true;
            return;
        }

        if (skipNextAttack) {
            skipNextAttack = false;
            return;
        }

        if (!config.isEnabled()
                || client.interactionManager == null
                || !player.isAlive()
                || player.isSpectator()) {
            return;
        }

        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            return;
        }

        Entity target = hit.getEntity();

        if (!TickDown.isValidTarget(
                client,
                target,
                config.getTargetMode()
        )) {
            return;
        }

        ItemStack weapon = player.getMainHandStack();

        String itemId = weapon.isEmpty()
                ? ""
                : Registries.ITEM.getId(weapon.getItem()).getPath();

        boolean isMace = itemId.equals("mace");
        boolean isAxe = itemId.endsWith("_axe");
        boolean isSword = itemId.endsWith("_sword");

        if (config.getWeaponMode() == TickWeapon.WEAPONS_ONLY
                && !isAllowedWeapon(weapon)) {
            return;
        }

        double maxDistance = config.getAttackDistance();
        Vec3d eyePos = player.getEyePos();
        Box targetBox = target.getBoundingBox();

        Vec3d closestPoint = new Vec3d(
                clamp(eyePos.x, targetBox.minX, targetBox.maxX),
                clamp(eyePos.y, targetBox.minY, targetBox.maxY),
                clamp(eyePos.z, targetBox.minZ, targetBox.maxZ)
        );

        if (eyePos.squaredDistanceTo(closestPoint)
                > maxDistance * maxDistance) {
            return;
        }

        if (!isMace && player.getAttackCooldownProgress(1.0F) < 1.0F) {
            return;
        }

        boolean smashReady = false;

        if (isMace) {
            if (maceCooldown > 0) {
                return;
            }

            smashReady = isSmashReady(player);

            // FAST mode: hit only when a smash attack is actually possible.
            if (config.getAttackMode() == TickMode.FAST && !smashReady) {
                return;
            }
        } else if (config.getAttackMode() == TickMode.FAST) {
            if (criticalAttackPerformed || !isCriticalTiming(player)) {
                return;
            }

            if (isAxe && player.getVelocity().y >= -0.08) {
                return;
            }
        }

        if (!isMace && !isSword && !isAxe) {
            return;
        }

        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);

        if (isMace) {
            maceCooldown = MACE_COOLDOWN_TICKS;

            if (smashReady) {
                maceHitThisFall = true;
                maceApexY = player.getY();
            }
        } else if (config.getAttackMode() == TickMode.FAST) {
            criticalAttackPerformed = true;
        }
    }

    /**
     * Follows the current fall: remembers the highest point of the jump and
     * re-arms the mace after the player starts rising again (wind charge,
     * Wind Burst) or lands. Without this the mace was locked until the
     * player touched the ground.
     */
    private void trackMaceFall(ClientPlayerEntity player) {
        if (maceCooldown > 0) {
            maceCooldown--;
        }

        double y = player.getY();
        double vy = player.getVelocity().y;

        boolean grounded = player.isOnGround()
                || player.isTouchingWater()
                || player.isClimbing()
                || player.hasVehicle()
                || player.isGliding();

        if (grounded || vy > 0.05) {
            maceHitThisFall = false;
        }

        if (grounded || vy >= 0.0) {
            maceApexY = y;
        } else if (y > maceApexY) {
            maceApexY = y;
        }
    }

    /**
     * Mace smash attack conditions: falling, more than 1.5 blocks fallen,
     * not gliding. Sprinting does not matter for the mace, so it is not
     * checked here (unlike regular critical hits).
     */
    private boolean isSmashReady(ClientPlayerEntity player) {
        if (maceHitThisFall
                || player.isOnGround()
                || player.isGliding()
                || player.hasVehicle()
                || player.isClimbing()
                || player.isTouchingWater()) {
            return false;
        }

        if (player.getVelocity().y >= 0.0) {
            return false;
        }

        double fallenFromApex = maceApexY - player.getY();

        return fallenFromApex > SMASH_FALL_DISTANCE
                && player.fallDistance > SMASH_FALL_DISTANCE;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isAllowedWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        String itemId = Registries.ITEM.getId(stack.getItem()).getPath();

        return itemId.endsWith("_sword")
                || itemId.endsWith("_axe")
                || itemId.equals("mace");
    }

    private static boolean isCriticalTiming(ClientPlayerEntity player) {
        return !player.isOnGround()
                && player.getVelocity().y < -0.08
                && player.fallDistance > 0.1F
                && !player.isSprinting()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasVehicle()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS);
    }
}
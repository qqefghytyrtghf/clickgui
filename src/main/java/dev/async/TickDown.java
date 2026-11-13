
package dev.async;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class TickDown {

    private TickDown() {
    }

    public static boolean isValidTarget(
            MinecraftClient client,
            Entity entity,
            TargetMode mode
    ) {
        ClientPlayerEntity owner = client.player;

        if (owner == null || entity == null || entity == owner) {
            return false;
        }

        if (!entity.isAlive() || entity.isRemoved() || !entity.isAttackable()) {
            return false;
        }

        return switch (mode) {
            case MOBS -> entity instanceof MobEntity;
            case PLAYERS -> entity instanceof PlayerEntity player
                    && !player.isSpectator();
        };
    }
}

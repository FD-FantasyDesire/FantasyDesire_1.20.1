package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.util.AttackHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

public class FDPlayerAttackHelper extends AttackHelper {
    public FDPlayerAttackHelper() {
    }

    public static void attack(LivingEntity attacker, Entity target, float comboRatio, DamageSource damageSource) {
        if (attacker instanceof Player player) {
            if (!ForgeHooks.onPlayerAttackTarget(player, target)) {
                return;
            }
        }

        if (target.isAttackable() && !target.skipAttackInteraction(attacker)) {
            boolean isCritical = isCriticalHit(attacker, target);
            double baseDamage = calculateTotalDamage(attacker, target, comboRatio, isCritical);
            if (!(baseDamage <= 0.0)) {
                float knockback = calculateKnockback(attacker);
                FireAspectResult fireAspectResult = handleFireAspect(attacker, target);
                Vec3 originalMotion = target.getDeltaMovement();
                boolean damageSuccess = target.hurt(damageSource, (float) baseDamage);
                if (damageSuccess) {
                    applyKnockback(attacker, target, knockback);
                    restoreTargetMotionIfNeeded(target, originalMotion);
                    playAttackEffects(attacker, target, isCritical);
                    handleEnchantmentsAndDurability(attacker, target);
                    handlePostAttackEffects(attacker, target, fireAspectResult);
                } else {
                    handleFailedAttack(attacker, target, fireAspectResult);
                }

            }
        }
    }

    public static void attack(LivingEntity attacker, Entity target, float comboRatio, double range,
            DamageSource damageSource) {
        if (attacker.distanceToSqr(target) <= range * range) {
            attack(attacker, target, comboRatio, damageSource);
        }
    }
}

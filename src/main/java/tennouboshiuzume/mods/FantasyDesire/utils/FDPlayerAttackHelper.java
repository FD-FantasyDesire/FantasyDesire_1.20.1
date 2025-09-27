package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.PlayerAttackHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.CriticalHitEvent;

public class FDPlayerAttackHelper extends PlayerAttackHelper {
    public FDPlayerAttackHelper() {
    }
    public static void attack(Player attacker, Entity target, float comboRatio, DamageSource damageSource) {
        if (ForgeHooks.onPlayerAttackTarget(attacker, target)) {
            if (target.isAttackable() && !target.skipAttackInteraction(attacker)) {
                float baseDamage = (float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
                baseDamage += getSweepingBonus(attacker);
                baseDamage += getRankBonus(attacker);
                baseDamage += getEnchantmentBonus(attacker, target);
                baseDamage = (float)((double)baseDamage * (double)(comboRatio * AttackManager.getSlashBladeDamageScale(attacker)) * (Double) SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER.get());
                if (!(baseDamage <= 0.0F)) {
                    float knockback = calculateKnockback(attacker);
                    boolean isCritical = isCriticalHit(attacker, target);
                    CriticalHitEvent hitResult = ForgeHooks.getCriticalHit(attacker, target, isCritical, isCritical ? 1.5F : 1.0F);
                    isCritical = hitResult != null;
                    if (isCritical) {
                        baseDamage *= hitResult.getDamageModifier();
                    }
                    FireAspectResult fireAspectResult = handleFireAspect(attacker, target);
                    Vec3 originalMotion = target.getDeltaMovement();
                    boolean damageSuccess = target.hurt(damageSource, baseDamage);
                    if (damageSuccess) {
                        applyKnockback(attacker, target, knockback);
                        restoreTargetMotionIfNeeded(target, originalMotion);
                        playAttackEffects(attacker, target, isCritical);
                        handleEnchantmentsAndDurability(attacker, target);
                        handlePostAttackEffects(attacker, target, fireAspectResult, isCritical);
                    } else {
                        handleFailedAttack(attacker, target, fireAspectResult);
                    }

                }
            }
        }
    }
}

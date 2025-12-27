package tennouboshiuzume.mods.FantasyDesire.utils;

import com.google.common.collect.Lists;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.ability.ArrowReflector;
import mods.flammpfeil.slashblade.ability.TNTExtinguisher;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class FDAttackManager extends AttackManager {

    public static List<Entity> areaAttack(LivingEntity playerIn, Consumer<LivingEntity> beforeHit,
            float comboRatio, boolean forceHit, boolean resetHit, boolean mute, List<Entity> exclude,
            @Nullable DamageSource type) {
        return areaAttack(playerIn, beforeHit, playerIn.position(), TargetSelector.getResolvedReach(playerIn),
                comboRatio, forceHit, resetHit, mute, exclude, type);
    }

    public static List<Entity> areaAttackWithSource(LivingEntity playerIn, Consumer<LivingEntity> beforeHit,
            float comboRatio, boolean forceHit, boolean resetHit, boolean mute, List<Entity> exclude,
            @Nullable DamageSource type) {
        return areaAttack(playerIn, beforeHit, comboRatio, forceHit, resetHit, mute, exclude, type);
    }

    public static List<Entity> areaAttackWithSource(LivingEntity playerIn, Consumer<LivingEntity> beforeHit,
            double radius, float comboRatio, boolean forceHit, boolean resetHit, boolean mute, List<Entity> exclude,
            @Nullable DamageSource type) {
        return areaAttack(playerIn, beforeHit, radius, comboRatio, forceHit, resetHit, mute, exclude, type);
    }

    public static List<Entity> areaAttack(LivingEntity playerIn, Consumer<LivingEntity> beforeHit,
            double radius, float comboRatio, boolean forceHit, boolean resetHit, boolean mute, List<Entity> exclude,
            @Nullable DamageSource type) {
        return areaAttack(playerIn, beforeHit, playerIn.position(), radius, comboRatio, forceHit, resetHit, mute,
                exclude, type);
    }

    public static List<Entity> areaAttack(LivingEntity playerIn, Consumer<LivingEntity> beforeHit,
            Vec3 center, double radius, float comboRatio, boolean forceHit, boolean resetHit, boolean mute,
            List<Entity> exclude,
            @Nullable DamageSource type) {
        List<Entity> founds = Lists.newArrayList();
        if (!playerIn.level().isClientSide()) {
            AABB aabb = new AABB(center.x - radius, center.y - radius, center.z - radius,
                    center.x + radius, center.y + radius, center.z + radius);

            founds = TargetSelector.getTargettableEntitiesWithinAABB(playerIn.level(), playerIn, aabb, radius);

            if (exclude != null) {
                founds.removeAll(exclude);
            }
            List<Entity> sphericalFounds = Lists.newArrayList();
            double radiusSqr = radius * radius;
            for (Entity entity : founds) {
                if (entity.distanceToSqr(center) <= radiusSqr) {
                    sphericalFounds.add(entity);
                }
            }
            founds = sphericalFounds;

            for (Entity entity : founds) {
                if (entity instanceof LivingEntity living) {
                    beforeHit.accept(living);
                }
                if (type != null) {
                    doMeleeAttackWithSource(playerIn, entity, forceHit, resetHit, comboRatio, type);
                } else {
                    doMeleeAttack(playerIn, entity, forceHit, resetHit, comboRatio);
                }
            }
        }

        if (!mute) {
            playerIn.level().playSound(null, center.x, center.y, center.z,
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F,
                    0.4F / (playerIn.getRandom().nextFloat() * 0.4F + 0.8F));
        }
        return founds;
    }

    public static void doMeleeAttackWithSource(LivingEntity attacker, Entity target, boolean forceHit, boolean resetHit,
            float comboRatio, @Nullable DamageSource type) {
        if (attacker instanceof Player) {
            doManagedAttack((t) -> {
                attacker.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent((state) -> {
                    try {
                        state.setOnClick(true);
                        if (type != null) {
                            FDPlayerAttackHelper.attack((Player) attacker, t, comboRatio, type);
                        } else {
                            FDPlayerAttackHelper.attack((Player) attacker, t, comboRatio);
                        }
                    } finally {
                        state.setOnClick(false);
                    }

                });
            }, target, forceHit, resetHit);
        } else {
            float baseAmount = (float) (attacker.getAttribute(Attributes.ATTACK_DAMAGE).getValue()
                    * (double) getSlashBladeDamageScale(attacker)
                    * (Double) SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER.get());
            doAttackWith(attacker.damageSources().mobAttack(attacker), baseAmount, target, forceHit, resetHit);
        }

        ArrowReflector.doReflect(target, attacker);
        TNTExtinguisher.doExtinguishing(target, attacker);
    }
}

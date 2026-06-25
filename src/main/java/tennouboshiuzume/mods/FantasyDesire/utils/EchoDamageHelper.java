package tennouboshiuzume.mods.FantasyDesire.utils;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.capability.EchoDamageProvider;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EchoDamageHelper {
    private static final String ECHO_DATA_TAG = "FDEchoDamageData";

    public static void addDamage(LivingEntity target, UUID attackerUUID, float amount) {
        CompoundTag persistentData = target.getPersistentData();
        CompoundTag echoData;
        if (persistentData.contains(ECHO_DATA_TAG)) {
            echoData = persistentData.getCompound(ECHO_DATA_TAG);
        } else {
            echoData = new CompoundTag();
        }

        String uuidStr = attackerUUID.toString();
        float currentDamage = echoData.getFloat(uuidStr);
        echoData.putFloat(uuidStr, currentDamage + amount);
        persistentData.put(ECHO_DATA_TAG, echoData);
    }

    public static float getTotalDamage(LivingEntity target, UUID attackerUUID) {
        CompoundTag persistentData = target.getPersistentData();
        if (persistentData.contains(ECHO_DATA_TAG)) {
            CompoundTag echoData = persistentData.getCompound(ECHO_DATA_TAG);
            String uuidStr = attackerUUID.toString();
            return echoData.getFloat(uuidStr);
        }
        return 0f;
    }

    public static void clearDamage(LivingEntity target, UUID attackerUUID) {
        CompoundTag persistentData = target.getPersistentData();
        if (persistentData.contains(ECHO_DATA_TAG)) {
            CompoundTag echoData = persistentData.getCompound(ECHO_DATA_TAG);
            String uuidStr = attackerUUID.toString();
            echoData.remove(uuidStr);
            if (echoData.isEmpty()) {
                persistentData.remove(ECHO_DATA_TAG);
            } else {
                persistentData.put(ECHO_DATA_TAG, echoData);
            }
        }
    }

    public static void detonateSingle(LivingEntity target) {
        target.getCapability(EchoDamageProvider.ECHO_DAMAGE).ifPresent(cap -> {
            Map<UUID, Float> damageMap = cap.getAllDamage();
            if (damageMap.isEmpty())
                return;

            boolean triggered = false;
            List<UUID> keysToRemove = new java.util.ArrayList<>();
            if (target.level() instanceof ServerLevel serverLevel) {
                List<Map.Entry<UUID, Float>> entriesToProcess = new java.util.ArrayList<>(damageMap.entrySet());
                for (Map.Entry<UUID, Float> entry : entriesToProcess) {
                    UUID attackerUUID = entry.getKey();
                    float damage = entry.getValue();

                    if (damage > 0) {
                        Entity attackerEntity = serverLevel.getEntity(attackerUUID);
                        if (attackerEntity != null) {
                            target.invulnerableTime = 0;
                            target.hurt(
                                    FDDamageSource.entityDamageSource(serverLevel, DamageTypes.GENERIC, attackerEntity),
                                    damage);
                            target.invulnerableTime = 0;
                            triggered = true;
                            keysToRemove.add(attackerUUID);
                        }
                    } else {
                        keysToRemove.add(attackerUUID);
                    }
                }
            }

            for (UUID key : keysToRemove) {
                damageMap.remove(key);
            }

            if (triggered) {
                target.playSound(SoundEvents.TRIDENT_RETURN, 1f, 1.5f);
                ParticleUtils.generateRingParticles(ParticleTypes.PORTAL, target.level(),
                        target.getX(),
                        target.getY() + target.getBbHeight() / 4, target.getZ(), 1, 4);
            }
        });
    }

    public static void detonateArea(LivingEntity centerEntity, double radius) {
        centerEntity.getCapability(EchoDamageProvider.ECHO_DAMAGE).ifPresent(cap -> {
            Map<UUID, Float> damageMap = cap.getAllDamage();
            if (damageMap.isEmpty())
                return;
            boolean triggered = false;
            List<UUID> keysToRemove = new java.util.ArrayList<>();
            if (centerEntity.level() instanceof ServerLevel serverLevel) {
                List<Map.Entry<UUID, Float>> entriesToProcess = new java.util.ArrayList<>(damageMap.entrySet());
                for (Map.Entry<UUID, Float> entry : entriesToProcess) {
                    float overflow = entry.getValue();
                    if (overflow > 0) {
                        try {
                            UUID attackerUUID = entry.getKey();
                            Entity attackerEntity = serverLevel.getEntity(attackerUUID);
                            if (attackerEntity != null) {
                                Vec3 position = centerEntity.position();
                                List<LivingEntity> nearbyEnemies = new java.util.ArrayList<>(
                                        FDTargetSelector.getLivingEntitiesInRadius(
                                                centerEntity,
                                                position,
                                                radius,
                                                false,
                                                null));
                                if (centerEntity.isAlive()) {
                                    nearbyEnemies.add(centerEntity);
                                }
                                nearbyEnemies = nearbyEnemies.stream()
                                        .filter(LivingEntity::isAlive)
                                        .toList();
                                if (!nearbyEnemies.isEmpty()) {
                                    float damagePerTarget = overflow / nearbyEnemies.size();
                                    for (LivingEntity enemy : nearbyEnemies) {
                                        enemy.invulnerableTime = 0;
                                        enemy.hurt(FDDamageSource.entityDamageSource(serverLevel,
                                                DamageTypes.MAGIC, attackerEntity), damagePerTarget);
                                        enemy.invulnerableTime = 0;
                                    }
                                }
                                triggered = true;
                                keysToRemove.add(attackerUUID);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    } else {
                        keysToRemove.add(entry.getKey());
                    }
                }
            }

            for (UUID key : keysToRemove) {
                damageMap.remove(key);
            }

            if (triggered) {
                centerEntity.playSound(SoundEvents.TRIDENT_RETURN, 1f, 1.5f);
                if (centerEntity.level() instanceof ServerLevel serverLevel) {
                    double yPos = centerEntity.getY() + centerEntity.getBbHeight() / 4;
                    tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils.sendForceParticles(serverLevel,
                            new tennouboshiuzume.mods.FantasyDesire.client.particle.FlatSpreadingRingParticleOptions(
                                    0x5500AA, (float) radius, 0.5f, 5),
                            centerEntity.getX(), yPos, centerEntity.getZ(),
                            1, 0, 0, 0, 0, 64.0);

                    for (int i = 0; i < 5; i++) {
                        double vx = (centerEntity.getRandom().nextDouble() - 0.5) * 0.5;
                        double vy = centerEntity.getRandom().nextDouble() * 0.5;
                        double vz = (centerEntity.getRandom().nextDouble() - 0.5) * 0.5;
                        serverLevel.sendParticles(
                                tennouboshiuzume.mods.FantasyDesire.init.FDParticles.ENDER_SHARD.get(),
                                centerEntity.getX(), yPos, centerEntity.getZ(),
                                0, vx, vy, vz, 1.0);
                    }
                }
            }
        });
    }
}

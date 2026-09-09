package tennouboshiuzume.mods.FantasyDesire.entity;

import java.util.List;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.client.particle.ColorShardParticle;
import tennouboshiuzume.mods.FantasyDesire.particle.ColorShardParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.particle.FlatSpreadingRingParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.particle.SpreadingRingParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;

public class EntityRefinedMissile extends EntityFDPhantomSword {
    // 绝肃·爆裂 智能弹头
    public EntityRefinedMissile(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void doExplosive() {
        if (this.level() instanceof ServerLevel serverLevel) {
            FlatSpreadingRingParticleOptions particleOptions = new FlatSpreadingRingParticleOptions(
                    getColor(), getExpRadius(), getExpRadius() * 0.1f, 20);
            Vec3 spawnPos = this.position();
            tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils.sendForceParticles(serverLevel, particleOptions,
                    spawnPos.x, spawnPos.y,
                    spawnPos.z, 1, 0, 0,
                    0, 0, 64.0);
            ColorShardParticleOptions shardOptions = new ColorShardParticleOptions(getColor(), getScale());
            serverLevel.sendParticles(shardOptions, this.getX(), this.getY(), this.getZ(), 20, 0, 0, 0,
                    0.1 * getExpRadius());
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0,
                    0,
                    0);
        }
        List<LivingEntity> enemies = FDTargetSelector.getNearbyLivingEntities(this, getExpRadius(), false,
                null);
        this.playSound(SoundEvents.GENERIC_EXPLODE, 3, 0.75f);
        for (LivingEntity target : enemies) {
            float damage = (float) this.getDamage() * getExpRadius();
            target.hurt(
                    FDDamageSource.entityDamageSource(this.level(), DamageTypes.PLAYER_EXPLOSION, this.getShooter()),
                    damage);
        }
    }

    @Override
    protected boolean doHurt(Entity target, DamageSource damagesource, float damageValue) {
        return target.hurt(damagesource, 1);
    }

    @Override
    public void customEffectFired() {
        if (this.level().isClientSide())
            return;

        Entity target = this.level().getEntity(this.getTargetId());
        if (target != null && !target.isAlive()) {
            if (getExpRadius() > 0) {
                doExplosive();
            }
            this.burst();
        }

        if (target != null && target.isAlive()) {
            double distSq = this.distanceToSqr(target);
            double dist = Math.sqrt(distSq);
            double minDist = 5.0;
            double maxDist = 35.0;
            double factor = (maxDist - dist) / (maxDist - minDist);
            factor = Math.max(0, Math.min(1, factor));
            float pitch = (float) (0.5 + 1.5 * factor);
            int maxInterval = 20;
            int minInterval = 2;
            int interval = (int) (maxInterval - (maxInterval - minInterval) * factor);
            interval = Math.max(1, interval);
            if (this.tickCount % 20 == 0) {
                if (!this.level().isClientSide && target instanceof LivingEntity livingTarget) {
                    MobEffectInstance existing = livingTarget.getEffect(FDPotionEffects.MISSILE_LOCKED.get());
                    if (existing != null) {
                        livingTarget.forceAddEffect(
                                new MobEffectInstance(
                                        FDPotionEffects.MISSILE_LOCKED.get(),
                                        60,
                                        existing.getAmplifier(),
                                        false,
                                        false,
                                        true),
                                this.getOwner());
                    }
                }
            }
            if (this.tickCount % interval == 0) {
                // 在最短追踪时飞行越久，伤害越高，最高3000点
                if (interval == 2) {
                    this.setDamage(Math.min(getDamage() * 1.05, 3000));
                    this.setExpRadius(Math.min(getExpRadius() * 1.05f, 5f));
                }
                this.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 1.0f, pitch);
                if (this.level() instanceof ServerLevel serverLevel) {
                    SpreadingRingParticleOptions particleOptions = new SpreadingRingParticleOptions(
                            getColor(), 1 * getScale(), 0.1f * getScale(), 60);
                    Vec3 spawnPos = this.position();
                    tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils.sendForceParticles(serverLevel,
                            particleOptions, spawnPos.x, spawnPos.y,
                            spawnPos.z, 1, 0, 0,
                            0, 0, 64.0);
                }
            }
        } else {
            if (this.tickCount % 20 == 0) {
                this.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 1.0f, 0.5f);
                if (this.level() instanceof ServerLevel serverLevel) {
                    SpreadingRingParticleOptions particleOptions = new SpreadingRingParticleOptions(
                            getColor(), 1 * getScale(), 0.1f * getScale(), 60);
                    Vec3 spawnPos = this.position();
                    tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils.sendForceParticles(serverLevel,
                            particleOptions, spawnPos.x, spawnPos.y,
                            spawnPos.z, 1, 0, 0,
                            0, 0, 64.0);
                }
            }
        }
    }
}

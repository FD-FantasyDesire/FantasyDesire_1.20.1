package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class EntityRefinedMissile extends EntityFDPhantomSword {
    // 绝肃·爆裂 智能弹头
    public EntityRefinedMissile(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void doExplosive() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 20, 0, 0, 0,
                    0.1 * getExpRadius());
        }
        super.doExplosive();
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
            if (this.tickCount % interval == 0) {
                // 在最短追踪时飞行越久，伤害越高，最高300点
                if (interval == 2) {
                    this.setDamage(Math.min(getDamage() * 1.05, 300));
                    this.setExpRadius(Math.min(getExpRadius() * 1.05f, 5f));
                }
                this.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 1.0f, pitch);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0,
                            0.05);
                }
            }
        } else {
            if (this.tickCount % 20 == 0) {
                this.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 1.0f, 0.5f);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0,
                            0.05);
                }
            }
        }
    }
}

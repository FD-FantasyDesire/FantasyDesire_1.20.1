package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.client.particle.FlatSpreadingRingParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EntityFDBFG extends EntityFDEnergyBullet {
    public List<LivingEntity> clientTargets = new ArrayList<>();

    public EntityFDBFG(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.tickCount % 2 == 0) {
            List<Entity> excludeList = new ArrayList<>();
            excludeList.add(this.getShooter());
            this.clientTargets = FDTargetSelector.getLivingEntitiesInRadius(this, this.position(), 25, false,
                    excludeList);
        }
    }

    // 非常！？DOOM？！的BFG
    @Override
    public void customEffectFired() {
        if (this.tickCount % 2 != 0)
            return;
        List<Entity> excludeList = new ArrayList<>();
        excludeList.add(this.getShooter());
        List<LivingEntity> targets = FDTargetSelector.getLivingEntitiesInRadius(this, this.position(), 25, false,
                excludeList);
        for (LivingEntity target : targets) {
            // Vec3 start = this.position();
            // Vec3 end = target.position().add(0, target.getBbHeight() / 2, 0);
            DamageSource damagesource;
            Entity shooter = this.getShooter();
            if (shooter == null) {
                damagesource = this.damageSources().indirectMagic(this, this);
            } else {
                damagesource = this.damageSources().indirectMagic(this, shooter);
            }
            target.invulnerableTime = 0; // 确保可以被高频攻击
            target.hurt(damagesource, (float) this.getDamage() * 0.2f); // 每次闪电造成20%伤害
            if (this.level() instanceof ServerLevel serverLevel) {
                // ParticleUtils.LightBoltParticles(serverLevel, start, end, 0x00FF00, 0.1f, 2,
                // 0.75f, false, 2, 8);
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.position().x,
                        target.position().y + target.getBbHeight() / 2, target.position().z, 5, 0, 0, 0, 0.5);
            }
        }
    }

    @Override
    protected void doExplosive() {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            float expRadius = this.getExpRadius();
            if (expRadius <= 0)
                return;
            int color = this.getColor();
            Vec3 center = this.position();
            FlatSpreadingRingParticleOptions options = new FlatSpreadingRingParticleOptions(color, expRadius, 0.5f, 10);
            tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils.sendForceParticles(serverLevel, options, center.x,
                    center.y, center.z, 1, 0, 0, 0, 0, 64.0);
            Entity shooterEntity = this.getShooter();
            List<Entity> excludes = new ArrayList<>();
            excludes.add(this);
            if (shooterEntity != null) {
                excludes.add(shooterEntity);
            }
            List<LivingEntity> targets = new ArrayList<>(
                    FDTargetSelector.getLivingEntitiesInRadius(
                            shooterEntity != null ? shooterEntity : this,
                            center,
                            expRadius,
                            true,
                            excludes));
            targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
            ParticleUtils.LightBoltParticles(
                    serverLevel,
                    center,
                    this.position().add(0, 20, 0),
                    color,
                    0.2f, // thickness
                    10, // lifetime
                    1.0f, // alpha
                    true, // fade
                    2.0, // randomness
                    8 // maxSegments
            );
            this.playSound(SoundEvents.GENERIC_EXPLODE, 3, 0.5f);
            for (LivingEntity target : targets) {
                // Deal damage
                Entity shooter = this.getShooter();
                DamageSource source = shooter == null ? this.damageSources().indirectMagic(this, this)
                        : this.damageSources().indirectMagic(this, shooter);
                target.invulnerableTime = 0;
                if (doExplosiveDamage(target, source)) {
                    // Create lightning particle lines
                    Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);
                    ParticleUtils.LightBoltParticles(
                            serverLevel,
                            center,
                            targetCenter,
                            color,
                            0.05f, // thickness
                            10, // lifetime
                            1.0f, // alpha
                            true, // fade
                            0.5, // randomness
                            3 // maxSegments
                    );
                }
            }
        }
    }

    @Override
    protected boolean doExplosiveDamage(LivingEntity target, DamageSource source) {
        int tickRemain = getDelay() - tickCount;
        float damage = (float) (this.getDamage() * 0.2f);
        int remainDamageTimes = tickRemain / 10;
        // 使其提前引爆时造成剩余全额伤害
        return target.hurt(source, remainDamageTimes * damage);
    }
}

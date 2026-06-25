package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.client.particle.FlatSpreadingRingParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.client.particle.SpreadingRingParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EntityFDEnergyBullet extends EntityFDPhantomSword {

    public EntityFDEnergyBullet(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    public void tick() {
        super.tick();
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
                            false,
                            excludes));
            // Sort by distance to the projectile
            targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
            int hitCount = 0;
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
                if (hitCount >= 5)
                    break;
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
                    hitCount++;
                }
            }
        }
    }

    protected boolean doExplosiveDamage(LivingEntity target, DamageSource source) {
        return target.hurt(source, (float) this.getDamage());
    }
}
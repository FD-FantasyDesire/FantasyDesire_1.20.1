package tennouboshiuzume.mods.FantasyDesire.entity;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.client.particle.SpreadingRingParticleOptions;
import mods.flammpfeil.slashblade.util.TargetSelector;

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

            // 1. Spreading ring particle
            SpreadingRingParticleOptions options = new SpreadingRingParticleOptions(color, expRadius, 0.5f, 10);
            serverLevel.sendParticles(options, center.x, center.y, center.z, 1, 0, 0, 0, 0);

            // 2. Find up to 5 entities within the ExpRadius
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
            for (LivingEntity target : targets) {
                if (hitCount >= 5)
                    break;

                // Deal damage
                Entity shooter = this.getShooter();
                DamageSource source = shooter == null ? this.damageSources().indirectMagic(this, this)
                        : this.damageSources().indirectMagic(this, shooter);

                target.invulnerableTime = 0;
                if (target.hurt(source, (float) this.getDamage())) {
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
}
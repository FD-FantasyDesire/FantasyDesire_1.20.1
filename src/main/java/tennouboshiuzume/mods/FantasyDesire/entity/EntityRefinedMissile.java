package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class EntityRefinedMissile extends EntityFDPhantomSword {

    public EntityRefinedMissile(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    public void customEffectFired() {
        if (this.level().isClientSide())
            return;

        Entity target = this.level().getEntity(this.getTargetId());
        if (target != null && target.isAlive()) {
            double distSq = this.distanceToSqr(target);
            double dist = Math.sqrt(distSq);

            double minDist = 10.0;
            double maxDist = 20.0;

            // 0 at maxDist (far), 1 at minDist (close)
            double factor = (maxDist - dist) / (maxDist - minDist);
            factor = Math.max(0, Math.min(1, factor));

            // Pitch: 0.5 (far) to 2.0 (close)
            float pitch = (float) (0.5 + 1.5 * factor);

            // Interval: 20 (far) to 2 (close)
            int maxInterval = 20;
            int minInterval = 2;
            int interval = (int) (maxInterval - (maxInterval - minInterval) * factor);
            interval = Math.max(1, interval);

            if (this.tickCount % interval == 0) {
                // Use a higher volume so it can be heard
                this.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 1.0f, pitch);
            }
        } else {
            if (this.tickCount % 20 == 0) {
                this.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 1.0f, 0.5f);
            }
        }
    }
}

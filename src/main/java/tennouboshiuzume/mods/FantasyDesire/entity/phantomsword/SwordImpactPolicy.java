package tennouboshiuzume.mods.FantasyDesire.entity.phantomsword;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;

public interface SwordImpactPolicy {
    SwordImpactPolicy LEGACY = new SwordImpactPolicy() {};

    default boolean canHit(EntityFDPhantomSword sword, Entity target) {
        return !(target instanceof LivingEntity living && sword.getShooter() instanceof LivingEntity shooter)
                || TargetSelector.test.test(shooter, living);
    }

    /** 在伤害和命中事件之前预留目标，防止回调重入；旧模式保留父类 N+1 的穿透额度。 */
    default boolean beginHit(EntityFDPhantomSword sword, Entity target) {
        if (sword.getPierce() <= 0)
            return true;
        if (sword.getAlreadyHits() == null)
            sword.setAlreadyHits(new IntOpenHashSet(5));
        if (sword.getAlreadyHits().contains(target.getId()))
            return false;
        if (sword.getAlreadyHits().size() >= sword.getPierce() + 1) {
            sword.burst();
            return false;
        }
        sword.getAlreadyHits().add(target.getId());
        return true;
    }

    default void afterHit(EntityFDPhantomSword sword, Entity target) {}

    default boolean canEmbed(EntityFDPhantomSword sword) {
        return sword.getPierce() <= 0;
    }

    default boolean continueSweep(EntityFDPhantomSword sword) {
        return sword.getPierce() > 0;
    }

    default boolean stopAtContact() {
        return false;
    }

    default void damageRejected(EntityFDPhantomSword sword) {
        if (sword.getDeltaMovement().lengthSqr() < 1.0E-7) {
            if (sword.getPierce() <= 1)
                sword.burst();
            else
                sword.setPierce((byte) (sword.getPierce() - 1));
        }
    }
}

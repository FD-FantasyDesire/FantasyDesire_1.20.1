package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.utils.FDAttackManager;

import java.util.List;

public class EntityEnderSlashEffect extends EntityFDSlashEffect {
    public EntityEnderSlashEffect(EntityType<? extends EntitySlashEffect> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void handleAttack() {
        if (this.tickCount % 2 == 0) {
            boolean forceHit = true;
            List<Entity> hits;
            if (!getIndirect() && getShooter() instanceof LivingEntity shooter) {
                float ratio = (float) getDamage() * (getIsCritical() ? 1.1f : 1.0f);
                hits = FDAttackManager.areaAttack(shooter, this.getAction().action, this.position(),
                        4.0 * this.getScale(), ratio,
                        forceHit, false, true,
                        getAlreadyHits(), FDDamageSource.getEntityDamageSource(shooter.level(),FDDamageSource.ECHO,shooter));
            } else {
                hits = AttackManager.areaAttack(this, this.getAction().action, 4.0 * this.getScale(), forceHit,
                        false,
                        getAlreadyHits());
            }

            if (!this.doCycleHit()) {
                getAlreadyHits().addAll(hits);
            }
        }
    }
}

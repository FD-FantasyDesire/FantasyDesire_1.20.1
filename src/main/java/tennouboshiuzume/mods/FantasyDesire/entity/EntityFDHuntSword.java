package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

public class EntityFDHuntSword extends EntityFDPhantomSword {

    public EntityFDHuntSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity targetEntity = entityHitResult.getEntity();
        if (targetEntity.isAlive()) {
            this.setHitEntity(targetEntity);
            this.setTailNodes(48);
        }
    }

    @Override
    protected void stabInEntity(Entity hit) {
        if (!this.level().isClientSide() && this.getHitEntity() != null && this.getHitEntity().isAlive()) {
            if (!this.getNoEvent()) {
                SlashBladeEvent.SummonedSwordOnHitEntityEvent event = new SlashBladeEvent.SummonedSwordOnHitEntityEvent(
                        this, this.getHitEntity());
                MinecraftForge.EVENT_BUS.post(event);
            }

            // 幻猎 幻影剑击中时拉近敌人
            if (this.getShooter() instanceof LivingEntity shooter
                    && this.getHitEntity() instanceof LivingEntity targetEntity) {
                if (shooter.distanceTo(targetEntity) > 5) {
                    Vec3 motion = shooter.position().subtract(targetEntity.position()).normalize().scale(0.8);
                    targetEntity.setDeltaMovement(motion);
                    targetEntity.hurtMarked = true;
                }
            }
        }
        super.stabInEntity(hit);
    }
}

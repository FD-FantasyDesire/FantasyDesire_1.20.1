package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.TargetUtils;

import java.util.ArrayList;
import java.util.List;

public class EntityFDBFG extends EntityFDEnergyBullet {
    public EntityFDBFG(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    public void customEffectFired() {
        List<Entity> excludeList = new ArrayList<>();
        excludeList.add(this);
        excludeList.add(this.getShooter());
        List<LivingEntity> targets = TargetUtils.getLivingEntitiesInRadius(this, this.position(), 25,false,excludeList);
        for (LivingEntity target : targets) {
            Vec3 start = this.position();
            Vec3 end = target.position().add(0, target.getBbHeight() / 2, 0);
            if (this.level() instanceof ServerLevel serverLevel) {
                ParticleUtils.LightBoltParticles(serverLevel,start,end,this.getColor(),0.1f,1,0.75f,false,2,8);
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,target.position().x,target.position().y+target.getBbHeight()/2,target.position().z,5,0,0,0,0.5);
            }
        }
    }

    ;


    @Override
    protected void onHitBlock(BlockHitResult blockraytraceresult) {
        if (this.getExpRadius() > 0) {
            this.level().explode(this.getShooter(), this.getX(), this.getY(), this.getZ(), this.getExpRadius(), Level.ExplosionInteraction.NONE);
            this.burst();
        }
    }
}

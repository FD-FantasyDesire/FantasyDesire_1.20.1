package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleTypes;
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
import tennouboshiuzume.mods.FantasyDesire.utils.TargetUtils;

import java.util.ArrayList;
import java.util.List;

public class EntityFDEnergyBullet extends EntityFDDriveEx{
    public EntityFDEnergyBullet(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity targetEntity = entityHitResult.getEntity();
        Entity shooter = this.getShooter();
        DamageSource damagesource;
        targetEntity.invulnerableTime = 0;
        if (shooter == null) {
            damagesource = this.damageSources().indirectMagic(this, this);
        } else {
            damagesource = this.damageSources().indirectMagic(this, shooter);
        }
        if (targetEntity.hurt(damagesource, (float) this.getDamage())){
            this.playSound(this.getHitEntitySound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            if (this.getExpRadius() > 0) {
                List<Entity> excludeList = new ArrayList<>();
                excludeList.add(this);
                excludeList.add(this.getShooter());
                List<LivingEntity> targets = TargetUtils.getNearbyLivingEntities(targetEntity, this.getExpRadius(),false,excludeList);
                for (LivingEntity target : targets) {
                    Vec3 start = targetEntity.position().add(0,target.getBbHeight()/2,0);
                    Vec3 end = target.position().add(0, target.getBbHeight() / 2, 0);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        ParticleUtils.LightBoltParticles(serverLevel,start,end,this.getColor(),0.05f,10,0.25f,false,2,4);
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,target.position().x,target.position().y+target.getBbHeight()/2,target.position().z,10,0,0,0,0.5);
                    }
                    target.invulnerableTime = 0;
                    target.hurt(damagesource, (float) this.getDamage());
                    target.invulnerableTime = 0;
                }
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockraytraceresult) {
        if (this.getExpRadius() > 0) {
            List<Entity> excludeList = new ArrayList<>();
            excludeList.add(this);
            excludeList.add(this.getShooter());
            List<LivingEntity> targets = TargetUtils.getLivingEntitiesInRadius(this, this.position(), this.getExpRadius(),false,excludeList);
            for (LivingEntity target : targets) {
                Vec3 start = this.position();
                Vec3 end = target.position().add(0, target.getBbHeight() / 2, 0);
                if (this.level() instanceof ServerLevel serverLevel) {
                    ParticleUtils.LightBoltParticles(serverLevel,start,end,this.getColor(),0.05f,10,0.25f,false,2,4);
                    serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,target.position().x,target.position().y+target.getBbHeight()/2,target.position().z,10,0,0,0,0.5);
                }
                Entity shooter = this.getShooter();
                DamageSource damagesource;
                if (shooter == null) {
                    damagesource = this.damageSources().indirectMagic(this, this);
                } else {
                    damagesource = this.damageSources().indirectMagic(this, shooter);
                }
                target.invulnerableTime = 0;
                target.hurt(damagesource, (float) this.getDamage());
                target.invulnerableTime = 0;
            }
        }
        this.burst();
    }
}

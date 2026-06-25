package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.client.particle.SpreadingRingParticleOptions;

//造成次元伤害的幻影剑，SA和某些SE使用
public class EntityFDSoulPhantomSword extends EntityFDPhantomSword {
    public EntityFDSoulPhantomSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void playparticle() {
        SpreadingRingParticleOptions particleOptions = new SpreadingRingParticleOptions(
                getColor(), 0.5f, 0.1f * getScale(), 20);
        if (!this.level().isClientSide() && this.getFired()) {
            Vec3 pos = this.position();
            tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils.sendForceParticles((ServerLevel) this.level(),
                    particleOptions, pos.x, pos.y, pos.z, 1, 0, 0,
                    0, 0, 64.0);
        }
    }

}

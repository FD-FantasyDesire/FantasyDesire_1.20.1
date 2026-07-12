package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.client.particle.SpreadingRingParticleOptions;

// 具有星型闪光的幻影魂剑
public class EntityFDSoulPhantomSword extends EntityFDPhantomSword {
    public EntityFDSoulPhantomSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    // ========== 十字星型闪光特效 ==========
    /** 闪光剩余 tick（客户端） */
    private int flashTicks = 0;
    /** 标记是否已在发射时触发过闪光 */
    private boolean flashOnFireTriggered = false;

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.flashTicks > 0) {
            this.flashTicks--;
        }
    }

    /**
     * 获取当前闪光剩余 tick
     */
    public int getFlashTicks() {
        return flashTicks;
    }

    @Override
    public void customEffectFired() {
        if (this.level().isClientSide()) {
            if (this.getHitEntity() != null && this.flashTicks <= 0) {
                this.flashTicks = 15;
            }
            if (this.getFired() && !flashOnFireTriggered) {
                this.flashTicks = 20;
                this.flashOnFireTriggered = true;
            }
        }
    }

    @Override
    protected void playparticle() {
        // SpreadingRingParticleOptions particleOptions = new
        // SpreadingRingParticleOptions(
        // getColor(), 0.5f, 0.1f * getScale(), 20);
        // if (!this.level().isClientSide() && this.getFired()) {
        // Vec3 pos = this.position();
        // tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils.sendForceParticles((ServerLevel)
        // this.level(),
        // particleOptions, pos.x, pos.y, pos.z, 1, 0, 0,
        // 0, 0, 64.0);
        // }
    }

}

package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.TargetUtils;

import java.util.ArrayList;
import java.util.List;

public class EntityFDRainbowPhantomSword extends EntityFDPhantomSword{
    public EntityFDRainbowPhantomSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    public void tick(){
        super.tick();
        if (getShooter()!=null) makeSurePierce();
        if (!getInGround() && (getPierce() > 0 || getHitEntity() == null)) playRainbowParticle();
    }
    private void playRainbowParticle() {
        if (this.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) this.level();

        // 上一 tick 位置
        double px = this.xo;
        double py = this.yo;
        double pz = this.zo;

        // 当前 tick 位置
        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();

        int hexColor = getColor();
        Vector3f color = hexToVector3f(hexColor);

        DustParticleOptions dust = new DustParticleOptions(color, getScale());
        // 插值生成粒子点
        int steps = (int) getSpeed(); // 轨迹分段数量，越大越密
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = Mth.lerp(t, px, cx);
            double y = Mth.lerp(t, py, cy);
            double z = Mth.lerp(t, pz, cz);

            sl.sendParticles(
                    dust,
                    x, y, z,
                    1,
                    0.25,
                    0.25,
                    0.25,
                    0.05
            );
        }
    }

    private void makeSurePierce() {
        if (getShooter().position().y+this.getDeltaMovement().length()<=this.position().y){
            this.setNoClip(true);
        }else {
            this.setNoClip(false);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        this.burst();
    }
    @Override
    protected void onHitBlock(BlockHitResult blockraytraceresult) {
        super.onHitBlock(blockraytraceresult);
        this.burst();
    }


    @Override
    public void burst(){
        RainbowShockwave();
        super.burst();
    }
    private void RainbowShockwave(){
        LivingEntity player = (LivingEntity) this.getShooter();
        if (player==null) return;
        for (int i = 0; i < 7; i++) {
            int cl = ColorUtils.getSmoothTransitionColor(i,7,true);
            EntitySlashEffect jc = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, player.level());
            Vec3 pos = this.position().add(new Vec3(0,0,jc.getBbHeight()/2).yRot(-(float) Math.toRadians(360 /7*i+player.getYRot())));
            jc.setPos(pos.x, pos.y+this.getBbHeight()/2, pos.z);
            jc.setOwner(null);
            jc.setYRot((float) 360 /7*i+this.getYRot());
            jc.setXRot(0);
            jc.setColor(cl);
            jc.setRotationRoll(0f);
            jc.setMute(true);
            jc.setIsCritical(false);
            jc.setDamage(0.5);
            jc.setKnockBack(KnockBacks.cancel);
            if (player != null) {
                player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
                    jc.setRank(rank.getRankLevel(player.level().getGameTime()));
                });
            }
            player.level().addFreshEntity(jc);
        }
        List<Entity> excludeList = new ArrayList<>();
        excludeList.add(player);
        List<LivingEntity> target = TargetUtils.getLivingEntitiesInRadius(this,this.position(), 5.0,false,excludeList);
        for (LivingEntity targetEntity : target){
            AttackManager.doMeleeAttack(player,targetEntity,true,true,3.5f);
        }
    }


    public static Vector3f hexToVector3f(int hexColor) {
        // 提取红绿蓝，每个0~255
        int r = (hexColor >> 16) & 0xFF;
        int g = (hexColor >> 8) & 0xFF;
        int b = hexColor & 0xFF;

        // 转换为 0~1 的浮点数
        return new Vector3f(r / 255f, g / 255f, b / 255f);
    }
}

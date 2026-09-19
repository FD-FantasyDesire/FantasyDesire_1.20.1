package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

import java.util.List;

import javax.annotation.Nullable;

public class CometElytraEffect extends MobEffect {
    public CometElytraEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF); // 纯白
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {

        if (entity.level().isClientSide)
            return;
        if (entity.isShiftKeyDown())
            return;
        if (!(entity instanceof Player player))
            return;
        if (!player.isFallFlying())
            return;
        // 原版烟花同款推进
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();
        // 推力随等级提升
        double thrust = 0.06 + amplifier * 0.02;
        Vec3 boosted = motion.add(
                look.x * thrust,
                look.y * thrust * 0.5,
                look.z * thrust);
        double maxSpeed = 2.5 + amplifier * 0.3;
        if (boosted.length() > maxSpeed) {
            boosted = boosted.normalize().scale(maxSpeed);
        }
        player.setDeltaMovement(boosted);
        player.hurtMarked = true;
        if (player.tickCount % 2 == 0) {
            astraTails(player);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    // 彗星尾迹，我觉得还行？不知道要不要加旋转和第三条浅蓝色尾迹，看反馈了
    private static void astraTails(LivingEntity entity) {
        Vec3 center = entity.position().add(0, entity.getBbHeight() / 2, 0);
        if (entity.isFallFlying()) {
            RandomSource random = entity.getRandom();
            Vec3 velocity = entity.getDeltaMovement();
            Vec3 tails = velocity.reverse().scale(10);
            float tailsLength = (float) Math.max(3f, tails.length());
            Vec3 tailStart = center.add(velocity.normalize().scale(0.5));
            Vec3 tailEnd = center.add(tails.normalize().scale(tailsLength + 0.5));
            Vec3 left = new Vec3(
                    velocity.z,
                    0,
                    -velocity.x).normalize().scale(1.5);
            Vec3 right = left.reverse();
            for (int i = 0; i < 3; i++) {
                ParticleUtils.AstraLightningParticles(entity.level(), tailStart.add(left),
                        tailEnd.add(left.scale(2)).offsetRandom(random, 5f), 0xFFFF00,
                        0.05f, 20, 1,
                        true, 2f, 4, 0.5f, random.nextLong());
                ParticleUtils.AstraLightningParticles(entity.level(), tailStart.add(right),
                        tailEnd.add(right.scale(2)).offsetRandom(random, 5f), 0x00FFFF,
                        0.05f, 20, 1,
                        true, 2f, 4, 0.5f, random.nextLong());
            }
        }
    }
}

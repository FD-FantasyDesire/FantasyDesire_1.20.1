package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

import javax.annotation.Nullable;

public class CometElytraEffect extends MobEffect {
    public CometElytraEffect() {
        super(MobEffectCategory.BENEFICIAL,0xFFFFFF); //纯白
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {

        if (entity.level().isClientSide) return;

        if (!(entity instanceof Player player)) return;

        if (!player.isFallFlying()) return;

        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();

        // 推力随等级提升
        double thrust = 0.06 + amplifier * 0.02;

        Vec3 boosted = motion.add(
                look.x * thrust,
                look.y * thrust * 0.5,
                look.z * thrust
        );

        // 限制最大速度
        double maxSpeed = 2.5 + amplifier * 0.3;

        if (boosted.length() > maxSpeed) {
            boosted = boosted.normalize().scale(maxSpeed);
        }

        player.setDeltaMovement(boosted);
        player.hurtMarked = true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    public @Nullable ResourceLocation getIcon() {
        // 这是 HUD 图标（左上角）显示使用的图标
        return new ResourceLocation(FantasyDesire.MODID, "textures/mob_effect/void_strike.png");
    }

    public static DustColorTransitionOptions dust = new DustColorTransitionOptions(new Vector3f(1.0f, 0f, 1.0f), new Vector3f().zero(), 3f);
}

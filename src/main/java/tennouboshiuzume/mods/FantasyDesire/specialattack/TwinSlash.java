package tennouboshiuzume.mods.FantasyDesire.specialattack;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.client.particle.GlowingLineParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.TargetUtils;

import java.util.List;

import static mods.flammpfeil.slashblade.ability.SlayerStyleArts.*;

;

public class TwinSlash {
    public static void RippedStep(LivingEntity player, ItemStack blade) {
        if (!(blade.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        if (!(player instanceof Player)) return;
        LivingEntity nearest = TargetUtils.getNearestTargetInSight((Player) player, 35, 25, true, null);
        if (state.getTargetEntity(player.level()) instanceof LivingEntity targeted) nearest = targeted;
        if (nearest == null) return;
        player.playSound(SoundEvents.GRASS_STEP, 1f, 0.7f);
        // 粒子（只在服务器执行）
        Vec3 teleportPos = calculateTeleportPosition(player, nearest);
        for (int i = 0; i < 8; i++) {
            Vec3 offset = new Vec3((Math.random() - 0.5), (Math.random() - 0.5), (Math.random() - 0.5));
            Vec3 start = player.position().add(0, player.getBbHeight() / 2, 0).add(offset);
            Vec3 end = teleportPos.add(0, player.getBbHeight() / 2, 0).add(offset);
            ParticleUtils.LightBoltParticles(player.level(), start, end, i % 2 == 0 ? 0x00C8FF : 0xFF0089, 0.05f, 20, 0.5f, true, 1, 12);
        }
        if ((player.level() instanceof ServerLevel serverlevel)) {
            if (isValidTeleportPosition(teleportPos)) {
                performTeleportation((Entity) player, serverlevel, teleportPos);
                applyPostTeleportEffects(player);
            }
            Vec3 pos = player.position().add(0, 0.25, 0);
            serverlevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 20, 0.1, 0.1, 0.1, 0.2);
        }
    }
    public static void DominateStep(LivingEntity player, ItemStack blade) {
        if (!(blade.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        if (!(player instanceof Player)) return;
        List<LivingEntity> targetList = TargetUtils.getNearbyLivingEntities(player, 15, true, null);
        LivingEntity target = targetList.isEmpty() ? null : targetList.get(player.getRandom().nextInt(targetList.size()));
        if (state.getTargetEntity(player.level()) instanceof LivingEntity targeted) target = targeted;
        if (target == null) return;
        player.playSound(SoundEvents.GRASS_STEP, 1f, 0.7f);
        // 粒子（只在服务器执行）
        Vec3 teleportPos = calculateTeleportPosition(player, target);
        for (int i = 0; i < 8; i++) {
            Vec3 offset = new Vec3((Math.random() - 0.5), (Math.random() - 0.5), (Math.random() - 0.5));
            Vec3 start = player.position().add(0, player.getBbHeight() / 2, 0).add(offset);
            Vec3 end = teleportPos.add(0, player.getBbHeight() / 2, 0).add(offset);
            ParticleUtils.LightBoltParticles(player.level(), start, end, i % 2 == 0 ? 0x00C8FF : 0xFF0089, 0.05f, 20, 0.5f, true, 1, 12);
        }
        if ((player.level() instanceof ServerLevel serverlevel)) {
            if (isValidTeleportPosition(teleportPos)) {
                performTeleportation((Entity) player, serverlevel, teleportPos);
                applyPostTeleportEffects(player);
                player.lookAt(EntityAnchorArgument.Anchor.EYES,target.getEyePosition());
            }
            Vec3 pos = player.position().add(0, 0.25, 0);
            serverlevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 20, 0.1, 0.1, 0.1, 0.2);
        }
    }


    public static void MoodSlash(LivingEntity player, ItemStack blade,float Xrot,float Yrot,float roll,float offset) {
        if (player.level().isClientSide()) return;
        if (!(blade.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        AddonSlashUtils.doAddonSlash(player, roll, player.getYRot() + Yrot, Xrot, state.getColorCode(), offset, Vec3.ZERO, false, false, 0.3f, KnockBacks.cancel);
    }

    public static void DoomSlash(LivingEntity player, ItemStack blade,float roll,float ratio) {
        if (!(blade.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        AddonSlashUtils.doAddonSlashWithEvent(player, roll, player.getYRot(), 0, state.getColorCode(), 0f, AttackManager.genRushOffset(player), false, false, ratio, KnockBacks.cancel);
    }
    public static void HitEffect(Entity entity, int ratio) {
        if ((entity.level() instanceof ServerLevel serverlevel)) {
            Vec3 offsetS = new Vec3((Math.random() - 0.5) * ratio, (Math.random() - 0.5) * ratio, (Math.random() - 0.5) * ratio);
            Vec3 start = entity.position().add(offsetS).add(0, entity.getBbHeight() / 2, 0);
            Vec3 end = entity.position().add(0, entity.getBbHeight() / 2, 0);
            Vec3 midPoint = start.add(end).scale(0.5);
            GlowingLineParticleOptions opts = new GlowingLineParticleOptions(start, end, Math.random() < 0.5 ? 0x00C8FF : 0xFF0089, 0.05f, 1, false, 20);
            serverlevel.sendParticles(opts, midPoint.x, midPoint.y, midPoint.z, 1, 0, 0, 0, 0);
        }
    }

    public static Vec3 calculateTeleportPosition(Entity entityIn, LivingEntity target) {
        return target.position().add(0.0, (double)target.getBbHeight() * 0.1, 0.0).add(target.getLookAngle().scale(2.0));
    }
}

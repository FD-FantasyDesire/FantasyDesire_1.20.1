package tennouboshiuzume.mods.FantasyDesire.specialattack;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialAttacks;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.client.particle.GlowingLineParticleOptions;

import java.util.List;

import static mods.flammpfeil.slashblade.ability.SlayerStyleArts.*;

;

public class TwinSlash {
    public static void RippedStep(LivingEntity player, ItemStack blade) {
        if (!(blade.getItem() instanceof ItemSlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        if (!(player instanceof Player))
            return;
        LivingEntity nearest = FDTargetSelector.getNearestTargetInSight((Player) player, 35, 25, true, null);
        if (state.getTargetEntity(player.level()) instanceof LivingEntity targeted)
            nearest = targeted;
        if (nearest == null)
            return;
        player.playSound(SoundEvents.GRASS_STEP, 1f, 0.7f);
        // 粒子（只在服务器执行）
        Vec3 teleportPos = calculateTeleportPosition(player, nearest);
        for (int i = 0; i < 8; i++) {
            Vec3 offset = new Vec3((Math.random() - 0.5), (Math.random() - 0.5), (Math.random() - 0.5));
            Vec3 start = player.position().add(0, player.getBbHeight() / 2, 0).add(offset);
            Vec3 end = teleportPos.add(0, player.getBbHeight() / 2, 0).add(offset);
            ParticleUtils.LightBoltParticles(player.level(), start, end, i % 2 == 0 ? 0x00C8FF : 0xFF0089, 0.05f, 20,
                    0.5f, true, 1, 12);
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
        if (!(blade.getItem() instanceof ItemSlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        if (!(player instanceof Player))
            return;
        List<LivingEntity> targetList = FDTargetSelector.getNearbyLivingEntities(player, 15, true, null);
        LivingEntity target = targetList.isEmpty() ? null
                : targetList.get(player.getRandom().nextInt(targetList.size()));
        if (state.getTargetEntity(player.level()) instanceof LivingEntity targeted)
            target = targeted;
        if (target == null)
            return;
        player.playSound(SoundEvents.GRASS_STEP, 1f, 0.7f);
        Vec3 teleportPos = calculateTeleportPosition(player, target);
        for (int i = 0; i < 8; i++) {
            Vec3 offset = new Vec3((Math.random() - 0.5), (Math.random() - 0.5), (Math.random() - 0.5));
            Vec3 start = player.position().add(0, player.getBbHeight() / 2, 0).add(offset);
            Vec3 end = teleportPos.add(0, player.getBbHeight() / 2, 0).add(offset);
            ParticleUtils.LightBoltParticles(player.level(), start, end, i % 2 == 0 ? 0x00C8FF : 0xFF0089, 0.05f, 20,
                    0.5f, true, 1, 12);
        }
        if ((player.level() instanceof ServerLevel serverlevel)) {
            // 粒子（只在服务器执行）
            if (isValidTeleportPosition(teleportPos)) {
                performTeleportation((Entity) player, serverlevel, teleportPos);
                applyPostTeleportEffects(player);
                player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            }
            Vec3 pos = player.position().add(0, 0.25, 0);
            serverlevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 20, 0.1, 0.1, 0.1, 0.2);
        }
    }

    public static void MoodSlash(LivingEntity player, ItemStack blade, float Xrot, float Yrot, float roll,
            float offset) {
        if (player.level().isClientSide())
            return;
        if (!(blade.getItem() instanceof ItemSlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        AddonSlashUtils.doAddonSlash(player, roll, player.getYRot() + Yrot, Xrot, state.getColorCode(), offset,
                Vec3.ZERO, false, false, 0.3f, KnockBacks.cancel);
    }

    public static void MoodFinalRuneSword(LivingEntity player, LivingEntity target, ItemStack blade) {
        if (blade.getItem() instanceof ItemFantasySlashBlade) {
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            RandomSource random = target.getRandom();
            float yaw = (float) random.nextInt(360);
            float pitch = (float) random.nextInt(90);
            float roll = (float) (random.nextInt(360) - 180);
            Vec3 basePos = new Vec3(0, 0, 1);
            Vec3 spawnPos = target.position().add(0, target.getBbHeight() / 2, 0)
                    .add(basePos
                            .xRot((float) Math.toRadians(pitch))
                            .yRot((float) Math.toRadians(yaw))
                            .scale(3f));
            Vec3 lookVec = target.position().add(0, target.getBbHeight() / 2, 0).subtract(spawnPos).normalize();
            float lookYaw = (float) (Math.atan2(-lookVec.x, lookVec.z) * (180f / Math.PI));
            float lookPitch = (float) (Math.asin(-lookVec.y) * (180f / Math.PI));
            EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());
            ss.setIsCritical(false);
            ss.setOwner(player);
            ss.setRoll(roll);
            ss.setDamage(player.getMaxHealth() * 0.25);
            ss.setSpeed(1);
            ss.setColor(state.getColorCode());
            ss.setStandbyMode("WORLD");
            ss.setMovingMode("NORMAL");
            ss.setDelay(100);
            ss.setParticleType(ParticleTypes.ENCHANT);
            ss.setDelayTicks(40);
            ss.setNoClip(true);
            ss.setHasTail(false);
            ss.setFireSound(SoundEvents.TRIDENT_THUNDER, 0.5f, 2f);
            ss.setScale(1);
            ss.setTargetId(target.getId());
            ss.setStandbyYawPitch(lookYaw, lookPitch);
            ss.setPos(spawnPos);
            player.level().addFreshEntity(ss);
        }
    }

    public static void DoomSlash(LivingEntity player, ItemStack blade, float roll, float ratio) {
        if (!(blade.getItem() instanceof ItemSlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        AddonSlashUtils.doAddonSlashWithEvent(player, roll, player.getYRot(), 0, state.getColorCode(), 0f,
                AttackManager.genRushOffset(player), false, false, ratio, KnockBacks.cancel);
    }

    public static void HitEffect(Entity entity, int ratio) {
        if ((entity.level() instanceof ServerLevel serverlevel)) {
            Vec3 offsetS = new Vec3((Math.random() - 0.5) * ratio, (Math.random() - 0.5) * ratio,
                    (Math.random() - 0.5) * ratio);
            Vec3 start = entity.position().add(offsetS).add(0, entity.getBbHeight() / 2, 0);
            Vec3 end = entity.position().add(0, entity.getBbHeight() / 2, 0);
            Vec3 midPoint = start.add(end).scale(0.5);
            GlowingLineParticleOptions opts = new GlowingLineParticleOptions(start, end,
                    Math.random() < 0.5 ? 0x00C8FF : 0xFF0089, 0.05f, 1, false, 20);
            serverlevel.sendParticles(opts, midPoint.x, midPoint.y, midPoint.z, 1, 0, 0, 0, 0);
        }
    }

    public static Vec3 calculateTeleportPosition(Entity entityIn, LivingEntity target) {
        return target.position().add(0.0, (double) target.getBbHeight() * 0.1, 0.0)
                .add(target.getLookAngle().scale(2.0));
    }

    public static boolean AntiNTR(LivingEntity entity) {
        if (!(entity instanceof Player player))
            return false;

        CapabilityUtils.BladeContext mainCtx = CapabilityUtils.SEConditionMatcher.of(player)
                .requireTranslation("item.fantasydesire.twin_blade")
                .match();

        CapabilityUtils.BladeContext offCtx = CapabilityUtils.SEConditionMatcher.of(player)
                .onlyOffhand()
                .requireTranslation("item.fantasydesire.twin_blade")
                .match();

        if (mainCtx == null || offCtx == null)
            return false;
        if (mainCtx.fantasyState.getSpecialType().equals(offCtx.fantasyState.getSpecialType()))
            return false;

        return true;
    }

    public static void ConvertForm(LivingEntity entity, ItemStack blade) {
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (state == null || fdState == null)
            return;

        if (!state.getTranslationKey().equals("item.fantasydesire.twin_blade"))
            return;

        if (fdState.getSpecialType().equals("TwinBladeL")) {
            state.setTexture(new ResourceLocation(FantasyDesire.MODID, "models/twinbladeright.png"));
            state.setSlashArtsKey(FDSpecialAttacks.TWIN_SYSTEM_R.getId());
            state.setColorCode(0xFF0089);
            fdState.setSpecialType("TwinBladeR");
        } else if (fdState.getSpecialType().equals("TwinBladeR")) {
            state.setTexture(new ResourceLocation(FantasyDesire.MODID, "models/twinbladeleft.png"));
            state.setSlashArtsKey(FDSpecialAttacks.TWIN_SYSTEM_L.getId());
            state.setColorCode(0x00C8FF);
            fdState.setSpecialType("TwinBladeL");
        }
        entity.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1, 2);
    }

}

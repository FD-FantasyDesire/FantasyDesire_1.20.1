package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.particle.GlowingLineParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDSlashArtRegistry;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

import java.util.List;
import java.util.Random;

import static mods.flammpfeil.slashblade.ability.SlayerStyleArts.*;

;

public class TwinSlash {
    // 数值来自 FDConfig（服务端同步配置），使用处实时读取
    private static final FDConfig.TwinSystemL TWIN_SYSTEM_L = FDConfig.TWIN_SYSTEM_L;
    private static final FDConfig.TwinSystemR TWIN_SYSTEM_R = FDConfig.TWIN_SYSTEM_R;

    public static void RippedStep(LivingEntity player, ItemStack blade) {
        if (!(blade.getItem() instanceof ItemSlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        if (!(player instanceof Player))
            return;
        LivingEntity nearest = FDTargetSelector.getNearestTargetInSight((Player) player,
                TWIN_SYSTEM_L.rippedRange(), TWIN_SYSTEM_L.rippedAngle(), true, null);
        if (state.getTargetEntity(player.level()) instanceof LivingEntity targeted)
            nearest = targeted;
        if (nearest == null)
            return;
        player.playSound(SoundEvents.GRASS_STEP, 1f, 0.7f);
        Vec3 teleportPos = calculateTeleportPosition(player, nearest);
        if ((player.level() instanceof ServerLevel serverlevel)) {
            for (int i = 0; i < 8; i++) {
                Vec3 offset = new Vec3((Math.random() - 0.5), (Math.random() - 0.5), (Math.random() - 0.5));
                Vec3 start = player.position().add(0, player.getBbHeight() / 2, 0).add(offset);
                Vec3 end = teleportPos.add(0, player.getBbHeight() / 2, 0).add(offset);
                ParticleUtils.LightBoltParticles(player.level(), start, end, i % 2 == 0 ? 0x00C8FF : 0xFF0089, 0.05f,
                        20,
                        0.5f, true, 1, 12);
            }
            if (isValidTeleportPosition(teleportPos)) {
                // 我不知道拔刀剑原作者为什么要这么写瞬步，但是一定有他的道理
                performTeleportation((Entity) player, serverlevel, teleportPos);
                applyPostTeleportEffects(player);
                player.lookAt(EntityAnchorArgument.Anchor.EYES, nearest.getEyePosition());
            }
            Vec3 pos = player.position().add(0, 0.05, 0);
            serverlevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 20, 0.1, 0.1, 0.1, 0.2);
        }
    }

    public static void DominateStep(LivingEntity player, ItemStack blade) {
        if (!(blade.getItem() instanceof ItemSlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        if (!(player instanceof Player))
            return;
        List<LivingEntity> targetList = FDTargetSelector.getNearbyLivingEntities(player,
                TWIN_SYSTEM_R.dominateRadius(), true, null);
        LivingEntity target = targetList.isEmpty() ? null
                : targetList.get(player.getRandom().nextInt(targetList.size()));
        if (state.getTargetEntity(player.level()) instanceof LivingEntity targeted)
            target = targeted;
        if (target == null)
            return;
        player.playSound(SoundEvents.GRASS_STEP, 1f, 0.7f);
        Vec3 teleportPos = calculateTeleportPosition(player, target);

        if ((player.level() instanceof ServerLevel serverlevel)) {
            for (int i = 0; i < 2; i++) {
                Vec3 offset = new Vec3((Math.random() - 0.5), (Math.random() - 0.5),
                        (Math.random() - 0.5));
                Vec3 start = player.position().add(0, player.getBbHeight() / 2,
                        0).add(offset);
                Vec3 end = teleportPos.add(0, player.getBbHeight() / 2, 0).add(offset);
                ParticleUtils.LightBoltParticles(player.level(), start, end, i % 2 == 0 ? 0x00C8FF : 0xFF0089, 0.05f,
                        20,
                        0.5f, true, 1, 12);
            }
            if (isValidTeleportPosition(teleportPos)) {
                performTeleportation((Entity) player, serverlevel, teleportPos);
                applyPostTeleportEffects(player);
                player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            }
            Vec3 pos = player.position().add(0, 0.05, 0);
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
                Vec3.ZERO, false, false, TWIN_SYSTEM_L.spinRatio(), KnockBacks.cancel);
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
            ss.setDamage(player.getMaxHealth() * TWIN_SYSTEM_R.runeDamageHealthRatio());
            ss.setSpeed(1);
            ss.setColor(state.getColorCode());
            ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.WORLD);
            ss.setMovingMode(EntityFDPhantomSword.MovingMode.NORMAL);
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

    public static void HitEffect(Entity entity, float radiusMult) {
        Random rd = new Random();
        float halfLength = (entity.getBbHeight() + entity.getBbWidth()) / 3 * radiusMult;
        Vec3 base = new Vec3(0, 0, halfLength)
                .yRot((float) Math.toRadians(rd.nextInt(360))).xRot((float) Math.toRadians(rd.nextInt(360)));
        Vec3 start = entity.position().add(base).add(0, entity.getBbHeight() / 2, 0);
        Vec3 end = entity.position().add(base.scale(-1)).add(0, entity.getBbHeight() / 2, 0);
        Boolean tColor = Math.random() < 0.5;
        ParticleUtils.spwanBladeRiftParticles(entity.level(), start, end, 20, 0.9f, 0.03f,
                tColor ? 0x00C8FF : 0xFF0089,
                tColor ? 0xFF0089 : 0x00C8FF);
    }

    public static Vec3 calculateTeleportPosition(Entity entityIn, LivingEntity target) {
        return target.position().add(0.0, (double) target.getBbHeight() * 0.1, 0.0)
                .add(target.getLookAngle().scale(TWIN_SYSTEM_R.teleportDistance()));
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
            state.setSlashArtsKey(FDSlashArtRegistry.TWIN_SYSTEM_R.getId());
            state.setColorCode(0xFF0089);
            fdState.setSpecialType("TwinBladeR");
        } else if (fdState.getSpecialType().equals("TwinBladeR")) {
            state.setTexture(new ResourceLocation(FantasyDesire.MODID, "models/twinbladeleft.png"));
            state.setSlashArtsKey(FDSlashArtRegistry.TWIN_SYSTEM_L.getId());
            state.setColorCode(0x00C8FF);
            fdState.setSpecialType("TwinBladeL");
        }
        entity.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1, 2);
    }

}

package tennouboshiuzume.mods.FantasyDesire.slasharts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.VectorHelper;
import net.minecraft.server.packs.resources.ResourceManager.Empty;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityEnderSlashEffect;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.particle.AstraStarParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.EchoDamageHelper;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

public class EchoingVoid {
    // 起手斩与后续四段范围斩的固定伤害，与 FDCombo 中的连段阶段一一对应。
    public static final float OPENING_SLASH_DAMAGE = 0.1F;
    public static final float FIRST_SLASH_DAMAGE = 3.0F;
    public static final float SECOND_SLASH_DAMAGE = 5.0F;
    public static final float THIRD_SLASH_DAMAGE = 7.0F;
    public static final float FOURTH_SLASH_DAMAGE = 9.0F;

    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.starless_night")
                .match() != null;
    }

    public static void astralLightningEmitter(LivingEntity entity) {
        for (int i = 0; i < 16; i++) {
            Vec3 start = entity.position().add(0,
                    entity.getBbHeight() / 2, 0);
            Vec3 end = new Vec3(0, 0, 16 + 1.5f);
            end = end.yRot((float) Math.toRadians(
                    Math.random() * 360f))
                    .xRot((float) Math.toRadians(
                            Math.random() * 360f));

            Vec3 finalStart = end.normalize().scale(1.5f).add(start); // 防止生成在玩家同坐标直接被星辰挡住脸
            Vec3 finalEnd = end.add(start);
            ParticleUtils.AstraLightningParticles(
                    entity.level(), finalStart,
                    finalEnd, 0x8000FF, 0.2f,
                    20, 0.75f, true, 2d,
                    8,
                    1f,
                    entity.level().random
                            .nextLong());
        }
    }

    public static void fallenStarEffect(LivingEntity entity) {
        RandomSource random = entity.level().getRandom();
        Vec3 center = entity.position().add(0, 48 + random.nextGaussian(), 0);
        List<Vec3> starMaps = new ArrayList<>();
        List<StarEdge> edges = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            // 落星 无伤害纯特效
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(random.nextDouble()) * 24.0;
            Vec3 start = center.add(
                    Math.cos(angle) * radius,
                    random.nextGaussian() * 1.0,
                    Math.sin(angle) * radius);
            Vec3 velVec3 = new Vec3(0, 0, 1)
                    .xRot((float) Math.toRadians(-90 + (random.nextBoolean() ? 1 : -1) * (5 + random.nextDouble() * 5)))
                    .yRot(random.nextFloat() * (float) (Math.PI * 2));
            float scaleTotal = Math.max(random.nextFloat(), 0.5f);
            starMaps.add(start);
            ParticleUtils.spawnAstraStar(
                    entity.level(),
                    start,
                    velVec3,
                    new AstraStarParticleOptions(0x8000FF, 4 * scaleTotal, 1 * scaleTotal, 0.75f, 8, random.nextInt(45),
                            40),
                    1 + random.nextFloat());
        }
        boolean[] connected = new boolean[starMaps.size()];
        // 起始之星
        int first = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < starMaps.size(); i++) {
            double distance = starMaps.get(i).distanceToSqr(center);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                first = i;
            }
        }
        connected[first] = true;
        // Prim 最小生成树
        while (edges.size() < starMaps.size() - 1) {
            int bestStart = -1;
            int bestEnd = -1;
            double bestDistance = Double.MAX_VALUE;
            for (int i = 0; i < starMaps.size(); i++) {
                if (!connected[i]) {
                    continue;
                }
                for (int j = 0; j < starMaps.size(); j++) {
                    if (connected[j]) {
                        continue;
                    }
                    Vec3 start = starMaps.get(i);
                    Vec3 end = starMaps.get(j);
                    double distance = start.distanceToSqr(end);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestStart = i;
                        bestEnd = j;
                    }
                }
            }
            if (bestStart == -1) {
                break;
            }
            edges.add(new StarEdge(
                    bestStart,
                    bestEnd));
            connected[bestEnd] = true;
        }
        for (int i = 0; i < edges.size(); i++) {
            StarEdge edge = edges.get(i);
            Vec3 start = starMaps.get(edge.start());
            Vec3 end = starMaps.get(edge.end());
            int lifetime = 20 + i * 2;
            ParticleUtils.AstraLightningParticles(
                    entity.level(),
                    start,
                    end,
                    0x8000FF,
                    0.2f,
                    lifetime,
                    0.75f,
                    true,
                    0,
                    2,
                    1f,
                    random.nextLong());
        }
    }

    private record StarEdge(int start, int end) {
    }

    public static void forceTriggerEchoDamage(LivingEntity entity) {
        List<LivingEntity> targets = FDTargetSelector
                .getLivingEntitiesInRadius(entity,
                        entity.position(),
                        40.0, false, null);
        for (LivingEntity target : targets) {
            EchoDamageHelper.detonateArea(target, 10.0);
            showCrossStarOnTrigger(target);
        }
    }

    public static void showCrossStarOnTrigger(LivingEntity entity) {
        RandomSource random = entity.getRandom();
        ParticleUtils.spawnAstraStar(
                entity.level(),
                entity.position().add(0, entity.getBbHeight() / 2, 0),
                Vec3.ZERO,
                new AstraStarParticleOptions(0x8000FF, 4, 0.5f, 0.75f, 0, random.nextInt(90),
                        20),
                0);
    }

    //
    public static EntitySlashEffect doEnderSlash(LivingEntity playerIn, float roll, float YRot, float XRot,
            int colorCode, float rotationOffset, Vec3 centerOffset, boolean mute, boolean critical, double damage,
            KnockBacks knockback, float scale, int lifetime) {
        if (playerIn.level().isClientSide()) {
            return null;
        } else {
            Vec3 pos = playerIn.position().add(0.0, (double) playerIn.getEyeHeight() * 0.75, 0.0)
                    .add(playerIn.getLookAngle().scale(0.30000001192092896));
            pos = pos.add(VectorHelper.getVectorForRotation(-90.0F, playerIn.getViewYRot(0.0F)).scale(centerOffset.y))
                    .add(VectorHelper.getVectorForRotation(0.0F, playerIn.getViewYRot(0.0F) + 90.0F)
                            .scale(centerOffset.z))
                    .add(playerIn.getLookAngle().scale(centerOffset.z));
            EntityEnderSlashEffect jc = new EntityEnderSlashEffect(FDEntitys.EnderSlashEffect.get(), playerIn.level());
            jc.setPos(pos.x, pos.y, pos.z);
            jc.setOwner(playerIn);
            jc.setLifetime(lifetime);
            jc.setRotationRoll(roll);
            jc.setYRot(YRot);
            jc.setXRot(XRot);
            jc.setRotationOffset(rotationOffset);
            jc.setColor(colorCode);
            jc.setMute(mute);
            jc.setIsCritical(critical);
            jc.setDamage(damage);
            jc.setKnockBack(knockback);
            jc.setScale(scale);
            if (playerIn != null) {
                playerIn.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
                    jc.setRank(rank.getRankLevel(playerIn.level().getGameTime()));
                });
            }
            playerIn.level().addFreshEntity(jc);
            return jc;
        }
    }
}

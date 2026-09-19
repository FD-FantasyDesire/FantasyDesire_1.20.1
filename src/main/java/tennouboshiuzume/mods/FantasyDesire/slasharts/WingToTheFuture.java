package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDSoulPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.utils.*;

import java.util.List;

public class WingToTheFuture {
        /** 每翼幻影剑数量，与羽翼布局及发射编排保持一致。 */
        private static final int MAX_FEATHER = 32;
        private static final int EXP_LEVEL_SQRT_OFFSET = 5;
        // 数值来自 FDConfig（服务端同步配置），使用处实时读取
        private static final FDConfig.WingToTheFuture WING_TO_THE_FUTURE = FDConfig.WING_TO_THE_FUTURE;
        private static final String CHIKEFLARE_KEY = "item.fantasydesire.chikeflare";

        public static boolean AntiNTR(LivingEntity entity) {
                return CapabilityUtils.SEConditionMatcher.of(entity)
                                .requireTranslation(CHIKEFLARE_KEY)
                                .match() != null;
        }

        // 重制羽翼幻影剑展开
        public static void WingToTheFuture(LivingEntity player, ItemStack blade) {
                CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                                .requireTranslation(CHIKEFLARE_KEY)
                                .match();
                if (ctx == null)
                        return;
                if (!(player instanceof Player))
                        return;
                ISlashBladeState state = ctx.state;
                int wingCount = Mth.clamp((int) (Math.sqrt(Math.abs(((Player) player).experienceLevel))
                                - EXP_LEVEL_SQRT_OFFSET), 1, 3);
                float baseModif = state.getDamage();
                float magicDamage = WING_TO_THE_FUTURE.swordDamageBase()
                                + (baseModif * WING_TO_THE_FUTURE.swordDamageAttackRatio());
                int countdown = 1;
                int maxFeather = MAX_FEATHER;

                List<LivingEntity> targets = FDTargetSelector.getTargetsInSight((Player) player,
                                WING_TO_THE_FUTURE.targetRange(), WING_TO_THE_FUTURE.targetAngle(), true, null);
                for (int i = 0; i < wingCount; i++) {
                        int count = 1;
                        for (int j = 1; j <= maxFeather; j++) {
                                count++;
                                countdown++;
                                boolean front = (count % 2 == 0);
                                int countdownValue = countdown / 2;
                                float baseRadius = 2.5f;
                                float progress = (float) j / (maxFeather - 1);
                                float xRotDeg = 60f - progress * 120f;
                                float yRotDeg = front ? 120f : -120f;
                                Vec3 base = new Vec3(0, 0, 1);
                                Vec3 sec = base
                                                .yRot((float) Math.toRadians(yRotDeg + i * (front ? 5f : -5f)))
                                                .xRot((float) Math.toRadians(xRotDeg - i * (5f)))
                                                .normalize()
                                                .scale(baseRadius * (float) Math.pow(1.05, j) - i * 0.25);
                                EntityFDSoulPhantomSword ss = new EntityFDSoulPhantomSword(
                                                FDEntitys.FDSoulPhantomSword.get(),
                                                player.level());
                                ss.setIsCritical(false);
                                ss.setOwner(player);
                                ss.setOffset(sec);
                                ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
                                ss.setColor(front ? 0xFFFF00 : 0x00FFFF);
                                ss.setRoll(front ? -45.0f : 45.0f);
                                ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.PLAYER);
                                ss.setMovingMode(EntityFDPhantomSword.MovingMode.ADV_SEEK);
                                ss.setSpeed(WING_TO_THE_FUTURE.speed());
                                ss.setStandbyYawPitch(-yRotDeg, xRotDeg);
                                ss.setPos(player.position());
                                ss.setDamage(magicDamage);
                                ss.setSeekDelay(20 + countdownValue + 5);
                                ss.setDelayTicks(20 + countdownValue);
                                ss.setDelay(100 + countdownValue);
                                ss.setScale(1.5f);
                                ss.setExpRadius(WING_TO_THE_FUTURE.expRadius());
                                ss.setHasTail(true);
                                ss.setNoClip(true);
                                if (state.getTargetEntity(player.level()) != null) {
                                        ss.setTargetId(state.getTargetEntityId());
                                } else if (!targets.isEmpty()) {
                                        ss.setTargetId(targets.get(countdownValue % targets.size()).getId());
                                }
                                ss.tryInit();
                                player.level().addFreshEntity(ss);
                        }
                }
        }

        // 不是对应的刀，转化
        public static void ConvertChikeFlare(LivingEntity player, ItemStack blade) {
                ItemStack newBlade = ItemUtils.dataBakeBlade(blade,
                                FantasyDesire.getBladeAsRegistry(player.level(),
                                                FantasySlashBladeBuiltInRegistry.ChikeFlare));
                player.setItemInHand(InteractionHand.MAIN_HAND, newBlade);
                player.playSound(SoundEvents.TRIDENT_THUNDER, 1, 0.5f);
                ParticleUtils.LightBoltParticles(player.level(), player.position(),
                                player.position().add(new Vec3(0, 32, 0)),
                                0xFFFFFF, 0.2f, 20, 0.75f, false, 4, 16);
                for (int i = 0; i < 16; i++) {
                        Vec3 end = new Vec3(0, 0, 16);
                        end = end.yRot((float) Math.toRadians(Math.random() * 360f))
                                        .xRot((float) Math.toRadians(Math.random() * 360f)).add(player.position());
                        ParticleUtils.LightBoltParticles(player.level(), player.position(), end, 0xFFFF00, 0.1f, 40,
                                        0.75f, false,
                                        2, 32);
                        ParticleUtils.LightBoltParticles(player.level(), player.position(), end, 0x00FFFF, 0.1f, 40,
                                        0.75f, false,
                                        2, 32);
                }
        }
}

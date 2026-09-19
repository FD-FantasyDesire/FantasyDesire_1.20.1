package tennouboshiuzume.mods.FantasyDesire.init;

import org.checkerframework.checker.units.qual.s;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.handler.FallHandler;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.TimeValueHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.slasharts.*;
import tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.starlessnight.StarlessNightEffects;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ItemUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

public class FDCombo extends ComboStateRegistry {
        // 数值来自 FDConfig（服务端同步配置），使用处实时读取
        private static final FDConfig.CometElytra COMET_ELYTRA = FDConfig.COMET_ELYTRA;
        private static final FDConfig.CrimsonStrikeSa CRIMSON_STRIKE_SA = FDConfig.CRIMSON_STRIKE_SA;
        private static final FDConfig.TwinSystemR TWIN_SYSTEM_R = FDConfig.TWIN_SYSTEM_R;
        private static final FDConfig.EchoingVoidSa ECHOING_VOID_SA = FDConfig.ECHOING_VOID_SA;

        public static final DeferredRegister<ComboState> FD_COMBO_STATES = DeferredRegister
                        .create(ComboState.REGISTRY_KEY, FantasyDesire.MODID);

        public static final RegistryObject<ComboState> WING_TO_THE_FUTURE = FD_COMBO_STATES
                        .register("wing_to_the_future", ComboState.Builder.newInstance().startAndEnd(0, 1).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation).next(entity -> {
                                                if (WingToTheFuture.AntiNTR(entity)) {
                                                        return entity.isFallFlying() && !entity.isShiftKeyDown()
                                                                        ? FantasyDesire.prefix(
                                                                                        "wing_to_the_future_elytra")
                                                                        : FantasyDesire.prefix(
                                                                                        "wing_to_the_future_ground");
                                                } else {
                                                        return FantasyDesire.prefix("chike_flare_convert");
                                                }
                                        }).nextOfTimeout(entity -> {
                                                if (WingToTheFuture.AntiNTR(entity)) {
                                                        return entity.isFallFlying() && !entity.isShiftKeyDown()
                                                                        ? FantasyDesire.prefix(
                                                                                        "wing_to_the_future_elytra")
                                                                        : FantasyDesire.prefix(
                                                                                        "wing_to_the_future_ground");
                                                } else {
                                                        return FantasyDesire.prefix("chike_flare_convert");
                                                }
                                        })::build);

        public static final RegistryObject<ComboState> CHIKE_FLARE_CONVERT = FD_COMBO_STATES.register(
                        "chike_flare_convert",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .clickAction(entity -> WingToTheFuture.ConvertChikeFlare(entity,
                                                        entity.getMainHandItem()))::build);

        public static final RegistryObject<ComboState> WING_TO_THE_FUTURE_GROUND = FD_COMBO_STATES.register(
                        "wing_to_the_future_ground",
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(15,
                                                        entity -> SlashBlade.prefix("none")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("wing_to_the_future_end"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(2, entityIn -> AttackManager.doSlash(entityIn, -30F,
                                                                        Vec3.ZERO, false, false, 0.1F))
                                                        .put(9, entityIn -> WingToTheFuture.WingToTheFuture(entityIn,
                                                                        entityIn.getMainHandItem()))
                                                        .build())
                                        .addHitEffect(StunManager::setStun)::build);

        public static final RegistryObject<ComboState> WING_TO_THE_FUTURE_ELYTRA = FD_COMBO_STATES.register(
                        "wing_to_the_future_elytra",
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> FantasyDesire.prefix("wing_to_the_future_end"))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("wing_to_the_future_end"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(1, entityIn -> {
                                                                entityIn.addEffect(new MobEffectInstance(
                                                                                FDPotionEffects.COMET_ELYTRA.get(),
                                                                                COMET_ELYTRA.baseDuration(), 0));
                                                        })
                                                        .build())
                                        .addHitEffect(StunManager::setStun)::build);

        public static final RegistryObject<ComboState> WING_TO_THE_FUTURE_END = FD_COMBO_STATES.register(
                        "wing_to_the_future_end",
                        ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(0, AttackManager::playQuickSheathSoundAction).build())
                                        .releaseAction(ComboState::releaseActionQuickCharge)::build);

        public static final RegistryObject<ComboState> CRIMSON_STRIKE = FD_COMBO_STATES.register("crimson_strike",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> CrimsonStrike.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("crimson_strike_0")
                                                        : SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> CrimsonStrike.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("crimson_strike_0")
                                                        : SlashBlade.prefix("none"))::build);

        public static final RegistryObject<ComboState> CRIMSON_STRIKE_0 = FD_COMBO_STATES.register("crimson_strike_0",
                        ComboState.Builder.newInstance().startAndEnd(2200, 2251).priority(50).speed(1.0F)
                                        .next(ComboState.TimeoutNext.buildFromFrame(16,
                                                        entity -> FantasyDesire.prefix("crimson_strike_1")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("crimson_strike_1"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(16, entityIn -> CrimsonStrike.ShootHunterSword(entityIn))
                                                        .build())
                                        .addHitEffect(StunManager::setStun)::build);

        // 爪刃斩 左
        public static final RegistryObject<ComboState> CRIMSON_STRIKE_1 = FD_COMBO_STATES.register("crimson_strike_1",
                        ComboState.Builder.newInstance().startAndEnd(1816, 1859).speed(6F).priority(50)
                                        .next(ComboState.TimeoutNext.buildFromFrame(18,
                                                        entity -> FantasyDesire.prefix("crimson_strike_2")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("crimson_strike_2"))
                                        .clickAction((entityIn) -> CrimsonStrike.doTripleAddonFDSlash(
                                                        entityIn,
                                                        22.5F,
                                                        entityIn.getYRot(), 0, 0xFF0000, 0,
                                                        Vec3.ZERO,
                                                        false, false, CRIMSON_STRIKE_SA.clawDamage(),
                                                        KnockBacks.cancel,
                                                        20))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .build())
                                        .addHitEffect((target, attacker) -> {
                                                StunManager.setStun(target, 40);
                                                CrimsonStrike.HitEffect(target, 3f);
                                        })::build);

        // 爪刃斩 右
        public static final RegistryObject<ComboState> CRIMSON_STRIKE_2 = FD_COMBO_STATES.register("crimson_strike_2",
                        ComboState.Builder.newInstance().startAndEnd(204, 218).speed(1.1F).priority(50)
                                        .next(ComboState.TimeoutNext.buildFromFrame(20,
                                                        entity -> FantasyDesire.prefix("crimson_strike_end")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("crimson_strike_end"))
                                        .clickAction((entityIn) -> CrimsonStrike.doTripleAddonFDSlash(
                                                        entityIn,
                                                        180F - 22.5F,
                                                        entityIn.getYRot(), 0, 0xFF0000, 0,
                                                        Vec3.ZERO,
                                                        false, false, CRIMSON_STRIKE_SA.clawDamage(),
                                                        KnockBacks.cancel,
                                                        15))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .build())
                                        .addHitEffect((target, attacker) -> {
                                                StunManager.setStun(target, 40);
                                                CrimsonStrike.HitEffect(target, 3f);
                                        })::build);

        public static final RegistryObject<ComboState> CRIMSON_STRIKE_END = FD_COMBO_STATES.register(
                        "crimson_strike_end",
                        ComboState.Builder.newInstance().startAndEnd(218, 281).priority(50).aerial()
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("crimson_strike_end2"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(0, AttackManager::playQuickSheathSoundAction).build())
                                        .releaseAction(ComboState::releaseActionQuickCharge)::build);

        public static final RegistryObject<ComboState> CRIMSON_STRIKE_END2 = FD_COMBO_STATES.register(
                        "crimson_strike_end2",
                        ComboState.Builder.newInstance().startAndEnd(281, 314).priority(50).aerial()
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(0, AttackManager::playQuickSheathSoundAction).build())
                                        .releaseAction(ComboState::releaseActionQuickCharge)::build);
        // 虹光星雨
        public static final RegistryObject<ComboState> RAINBOW_STAR = FD_COMBO_STATES.register("rainbow_star",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> RainbowStar.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("rainbow_star_0")
                                                        : SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> RainbowStar.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("rainbow_star_0")
                                                        : SlashBlade.prefix("none"))::build);

        public static final RegistryObject<ComboState> RAINBOW_STAR_0 = FD_COMBO_STATES.register("rainbow_star_0",
                        ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(15,
                                                        entity -> SlashBlade.prefix("none")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("rainbow_star_end"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(2, entityIn -> AttackManager.doSlash(entityIn, -30F,
                                                                        Vec3.ZERO, false, false, 0.1F))
                                                        .put(3, entityIn -> RainbowStar.RainbowStar(entityIn,
                                                                        entityIn.getMainHandItem()))
                                                        .build())
                                        .addHitEffect(StunManager::setStun)::build);

        public static final RegistryObject<ComboState> RAINBOW_STAR_END = FD_COMBO_STATES.register("rainbow_star_end",
                        ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(0, AttackManager::playQuickSheathSoundAction).build())
                                        .releaseAction(ComboState::releaseActionQuickCharge)::build);

        // 以下是双刃SA和机制部分

        // 双子圣灵 切换形态
        public static final RegistryObject<ComboState> TWIN_MODE = FD_COMBO_STATES.register("twin_mode",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(80)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .clickAction(entity -> TwinSlash.ConvertForm(entity,
                                                        entity.getMainHandItem()))::build);

        // 启动程式：MOOD
        // 参考自黑兽·卯 奥义 云解显现
        // 蓄力，瞬步跃升斩将敌人上斩滞空
        // 回旋斩击2次后重锤落
        // 并且召唤幻影剑追加攻击

        // MOOD 施放前检测
        public static final RegistryObject<ComboState> MOOD_SLASH = FD_COMBO_STATES.register("mood_slash",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(80)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> TwinSlash.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("mood_slash_0")
                                                        : FantasyDesire.prefix("twin_mode"))
                                        .nextOfTimeout(entity -> TwinSlash.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("mood_slash_0")
                                                        : FantasyDesire.prefix("twin_mode"))::build);
        // 1段，滑步突击
        public static final RegistryObject<ComboState> MOOD_SLASH_0 = FD_COMBO_STATES.register("mood_slash_0",
                        ComboState.Builder.newInstance().startAndEnd(1, 33).priority(80)
                                        .motionLoc(DefaultResources.testLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(31,
                                                        entity -> FantasyDesire.prefix("mood_slash_1")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("mood_slash_1"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put((int) TimeValueHelper.getTicksFromFrames(30), entityIn -> {
                                                                TwinSlash.RippedStep(entityIn,
                                                                                entityIn.getMainHandItem());
                                                        }).build())::build);
        // 2段，跃升斩
        public static final RegistryObject<ComboState> MOOD_SLASH_1 = FD_COMBO_STATES.register("mood_slash_1",
                        ComboState.Builder.newInstance().startAndEnd(1700, 1713).priority(80)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(8,
                                                        entity -> FantasyDesire.prefix("mood_slash_2")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("mood_slash_2"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put((int) TimeValueHelper.getTicksFromFrames(7), entityIn -> {
                                                                Vec3 motion = entityIn.getDeltaMovement();
                                                                entityIn.setDeltaMovement(motion.x, 0.6f, motion.z);
                                                                AttackManager.doSlash(entityIn, -90 + 10, Vec3.ZERO,
                                                                                false, false,
                                                                                TwinSlash.MOOD_RISE_DAMAGE_RATIO,
                                                                                KnockBacks.toss);
                                                                AttackManager.doSlash(entityIn, -90 - 10, Vec3.ZERO,
                                                                                false, false,
                                                                                TwinSlash.MOOD_RISE_DAMAGE_RATIO,
                                                                                KnockBacks.toss);
                                                        }).build())
                                        .addHitEffect((target, attacker) -> {
                                                target.setDeltaMovement(0, 0.6f, 0);
                                                target.addEffect(
                                                                new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
                                                StunManager.setStun(target, 15);
                                                TwinSlash.HitEffect(target, 1.5f);
                                        })::build);
        // 3段，双旋斩
        public static final RegistryObject<ComboState> MOOD_SLASH_2 = FD_COMBO_STATES.register("mood_slash_2",
                        ComboState.Builder.newInstance().startAndEnd(725, 743).priority(80)
                                        .next(ComboState.TimeoutNext.buildFromFrame(12,
                                                        entity -> FantasyDesire.prefix("mood_slash_3")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("mood_slash_3"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(1, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getMainHandItem(), 0, 0, 45, 72))
                                                        .put(2, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getMainHandItem(), 0, 0, 45, 0))
                                                        .put(3, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getMainHandItem(), 0, 0, 45, -72))
                                                        .put(4, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getMainHandItem(), 0, 0, 45, -72 * 2))
                                                        .put(5, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getMainHandItem(), 0, 0, 45, -72 * 3))
                                                        .put(6, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getOffhandItem(), 0, 0, 135, 72))
                                                        .put(7, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getOffhandItem(), 0, 0, 135, 0))
                                                        .put(8, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getOffhandItem(), 0, 0, 135, -72))
                                                        .put(9, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getOffhandItem(), 0, 0, 135, -72 * 2))
                                                        .put(10, entityIn -> TwinSlash.MoodSlash(entityIn,
                                                                        entityIn.getOffhandItem(), 0, 0, 135, -72 * 3))
                                                        .build())
                                        .addHitEffect((target, attacker) -> {
                                                StunManager.setStun(target, 40);
                                                TwinSlash.HitEffect(target, 1.5f);
                                        }).addTickAction(FallHandler::fallDecrease)::build);
        // 4段，重锤落斩 + 幻影剑追击
        public static final RegistryObject<ComboState> MOOD_SLASH_3 = FD_COMBO_STATES.register("mood_slash_3",
                        ComboState.Builder.newInstance().startAndEnd(500, 576).priority(80)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder().put(8, entityIn -> {
                                                AttackManager.doSlash(entityIn, 90 - 15, false, false,
                                                                TwinSlash.SLAM_DAMAGE_RATIO);
                                                AttackManager.doSlash(entityIn, 90 + 15, true, false,
                                                                TwinSlash.SLAM_DAMAGE_RATIO);
                                                entityIn.moveRelative(entityIn.isInWater() ? 0.35f : 0.8f,
                                                                new Vec3(0, -0.5, 1.25));
                                                TwinSlash.ConvertForm(entityIn, entityIn.getMainHandItem());
                                                TwinSlash.ConvertForm(entityIn, entityIn.getOffhandItem());
                                        }).build()).addTickAction(FallHandler::fallDecrease)
                                        .addHitEffect((target, attacker) -> {
                                                StunManager.setStun(target, 40);
                                                TwinSlash.HitEffect(target, 4f);
                                                TwinSlash.MoodFinalRuneSword(attacker, target,
                                                                attacker.getMainHandItem());
                                                TwinSlash.MoodFinalRuneSword(attacker, target,
                                                                attacker.getOffhandItem());
                                        })::build);

        // 终结程式：DOOM
        // 参考自血天下鸡舞乱刀
        // 施放后瞬移自动锁定15m内敌人
        // 主动输入攻击键可以循环2，3连段
        // 每次输入攻击循环会烧血
        // 直到停止输入或者玩家低于50%血量

        // DOOM 施放前检测
        public static final RegistryObject<ComboState> DOOM_SLASH = FD_COMBO_STATES.register("doom_slash",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> TwinSlash.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("doom_slash_0")
                                                        : FantasyDesire.prefix("twin_mode"))
                                        .nextOfTimeout(entity -> TwinSlash.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("doom_slash_0")
                                                        : FantasyDesire.prefix("twin_mode"))::build);
        // 1段，闪击
        public static final RegistryObject<ComboState> DOOM_SLASH_0 = FD_COMBO_STATES.register("doom_slash_0",
                        ComboState.Builder.newInstance().startAndEnd(1, 33).priority(100)
                                        .motionLoc(DefaultResources.testLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(32,
                                                        entity -> FantasyDesire.prefix("doom_slash_1")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("doom_slash_1"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put((int) TimeValueHelper.getTicksFromFrames(30), entityIn -> {
                                                                TwinSlash.DominateStep(entityIn,
                                                                                entityIn.getMainHandItem());
                                                        }).build())::build);
        // 2段，预热
        public static final RegistryObject<ComboState> DOOM_SLASH_1 = FD_COMBO_STATES.register("doom_slash_1",
                        ComboState.Builder.newInstance().startAndEnd(700, 720).priority(100)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(13,
                                                        entity -> FantasyDesire.prefix("doom_slash_2")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("doom_slash_2"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder().put(6, entityIn -> {
                                                TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -30,
                                                                TwinSlash.DOOM_SLASH_DAMAGE_RATIO);
                                                TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 180 - 35,
                                                                TwinSlash.DOOM_SLASH_DAMAGE_RATIO);
                                        }).put(7, entityIn -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(),
                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(8, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(9, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(10, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(11, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(12, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(13, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(14, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .build())
                                        .addHitEffect((target, attacker) -> {
                                                StunManager.setStun(target, 40);
                                                TwinSlash.HitEffect(target, 1.5f);
                                        })::build);
        // 3段，循环狂热
        public static final RegistryObject<ComboState> DOOM_SLASH_2 = FD_COMBO_STATES.register("doom_slash_2",
                        ComboState.Builder.newInstance().startAndEnd(710, 720).priority(80)
                                        .next(ComboState.TimeoutNext.buildFromFrame(5,
                                                        entity -> entity.getHealth() > entity.getMaxHealth()
                                                                        * TwinSlash.DOOM_HEALTH_THRESHOLD
                                                                                        ? FantasyDesire.prefix(
                                                                                                        "doom_slash_3")
                                                                                        : FantasyDesire.prefix(
                                                                                                        "doom_slash_4")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("doom_slash_4"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder().put(0, entityIn -> {
                                                TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(),
                                                                -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                TwinSlash.DOOM_SLASH_DAMAGE_RATIO);
                                                TwinSlash.DominateStep(entityIn, entityIn.getMainHandItem());
                                                if (entityIn instanceof Player player) {
                                                        player.hurt(player.damageSources().playerAttack(player),
                                                                        TWIN_SYSTEM_R.doomSelfDamage());
                                                }
                                        }).put(1, entityIn -> {
                                                TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(),
                                                                90 + 180 * entityIn.getRandom().nextFloat(),
                                                                TwinSlash.DOOM_SLASH_DAMAGE_RATIO);
                                                entityIn.moveRelative(entityIn.isInWater() ? 0.35f : 0.8f,
                                                                new Vec3(0, 0, 4.5f));
                                        })
                                                        .put(2, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(3, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(4, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(5, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(6, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .build())
                                        .addHitEffect((target, attacker) -> {
                                                StunManager.setStun(target, 40);
                                                TwinSlash.HitEffect(target, 1.5f);
                                        })::build);
        // 4段，循环狂热
        public static final RegistryObject<ComboState> DOOM_SLASH_3 = FD_COMBO_STATES.register("doom_slash_3",
                        ComboState.Builder.newInstance().startAndEnd(710, 720).priority(80)
                                        .next(ComboState.TimeoutNext.buildFromFrame(5,
                                                        entity -> entity.getHealth() > entity.getMaxHealth()
                                                                        * TwinSlash.DOOM_HEALTH_THRESHOLD
                                                                                        ? FantasyDesire.prefix(
                                                                                                        "doom_slash_2")
                                                                                        : FantasyDesire.prefix(
                                                                                                        "doom_slash_4")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("doom_slash_4"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder().put(0, entityIn -> {
                                                TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(),
                                                                -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                TwinSlash.DOOM_SLASH_DAMAGE_RATIO);
                                                TwinSlash.DominateStep(entityIn, entityIn.getMainHandItem());
                                                if (entityIn instanceof Player player) {
                                                        player.hurt(player.damageSources().inFire(),
                                                                        TWIN_SYSTEM_R.doomSelfDamage());
                                                }
                                        }).put(1, entityIn -> {
                                                TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(),
                                                                90 + 180 * entityIn.getRandom().nextFloat(),
                                                                TwinSlash.DOOM_SLASH_DAMAGE_RATIO);
                                                entityIn.moveRelative(entityIn.isInWater() ? 0.35f : 0.8f,
                                                                new Vec3(0, 0, 4.5f));
                                        })
                                                        .put(2, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(3, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(4, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(5, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .put(6, entityIn -> TwinSlash.DoomSlash(entityIn,
                                                                        entityIn.getMainHandItem(),
                                                                        -90 + 180 * entityIn.getRandom().nextFloat(),
                                                                        TwinSlash.DOOM_SLASH_DAMAGE_RATIO))
                                                        .build())
                                        .addHitEffect((target, attacker) -> {
                                                StunManager.setStun(target, 40);
                                                TwinSlash.HitEffect(target, 1.5f);
                                        })::build);
        // 5段，重锤落
        public static final RegistryObject<ComboState> DOOM_SLASH_4 = FD_COMBO_STATES.register("doom_slash_4",
                        ComboState.Builder.newInstance().startAndEnd(500, 576).priority(80)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(26,
                                                        entity -> SlashBlade.prefix("none")))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder().put(8, entityIn -> {
                                                AttackManager.doSlash(entityIn, 90 - 15, false, false,
                                                                TwinSlash.SLAM_DAMAGE_RATIO);
                                                AttackManager.doSlash(entityIn, 90 + 15, true, false,
                                                                TwinSlash.SLAM_DAMAGE_RATIO);
                                                entityIn.moveRelative(entityIn.isInWater() ? 0.35f : 0.8f,
                                                                new Vec3(0, -0.5, 5.25));
                                                TwinSlash.ConvertForm(entityIn, entityIn.getMainHandItem());
                                                TwinSlash.ConvertForm(entityIn, entityIn.getOffhandItem());
                                        }).build()).addHitEffect((target, attacker) -> {
                                                StunManager.setStun(target, 40);
                                                TwinSlash.HitEffect(target, 5f);
                                        })::build);
        // 虚空回响
        public static final RegistryObject<ComboState> ECHOING_VOID = FD_COMBO_STATES.register("echoing_void",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> EchoingVoid.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("echoing_void_0")
                                                        : SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> EchoingVoid.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("echoing_void_0")
                                                        : SlashBlade.prefix("none"))::build);

        public static final RegistryObject<ComboState> ECHOING_VOID_0 = FD_COMBO_STATES.register("echoing_void_0",
                        ComboState.Builder.newInstance().startAndEnd(1, 33).priority(50)
                                        .motionLoc(DefaultResources.testLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(33,
                                                        entity -> FantasyDesire.prefix("echoing_void_1")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("echoing_void_1"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put((int) TimeValueHelper.getTicksFromFrames(30), entityIn -> {
                                                                ItemUtils.ConvertModel(entityIn.getMainHandItem(),
                                                                                "models/sn_huge.obj");
                                                                entityIn.playSound(SoundEvents.TRIDENT_THUNDER, 1.0f,
                                                                                1.5f);
                                                                EchoingVoid.astralLightningEmitter(entityIn);
                                                        }).build())::build);

        public static final RegistryObject<ComboState> ECHOING_VOID_1 = FD_COMBO_STATES.register("echoing_void_1",
                        ComboState.Builder.newInstance().startAndEnd(200, 218).priority(50)
                                        .next(ComboState.TimeoutNext.buildFromFrame(18,
                                                        entity -> FantasyDesire.prefix("echoing_void_2")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("echoing_void_2"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder().put(
                                                        (int) TimeValueHelper.getTicksFromFrames(6),
                                                        entityIn -> AddonSlashUtils.doAddonFDSlash(entityIn,
                                                                        180 - 42,
                                                                        entityIn.getYRot(), 0, 0x8000FF, 0, Vec3.ZERO,
                                                                        false, false,
                                                                        EchoingVoid.OPENING_SLASH_DAMAGE,
                                                                        KnockBacks.cancel, 10f, 10,
                                                                        FDDamageSource.ECHO.location().toString()))
                                                        .build())
                                        .addHitEffect((target, attacker) -> {
                                                StarlessNightEffects.stackVoidStrike(target,
                                                                ECHOING_VOID_SA.voidStrikeStacks());
                                        })::build);

        public static final RegistryObject<ComboState> ECHOING_VOID_2 = FD_COMBO_STATES.register("echoing_void_2",
                        ComboState.Builder.newInstance().startAndEnd(725, 743).priority(50)
                                        .next(ComboState.TimeoutNext.buildFromFrame(18,
                                                        entity -> FantasyDesire.prefix("echoing_void_end")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("echoing_void_end"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(4, (entityIn) -> EchoingVoid.doEnderSlash(
                                                                        entityIn,
                                                                        0,
                                                                        entityIn.getYRot() + 180, 0, 0x8000FF, 0,
                                                                        Vec3.ZERO,
                                                                        false, false,
                                                                        EchoingVoid.FIRST_SLASH_DAMAGE,
                                                                        KnockBacks.cancel, 10f,
                                                                        10))
                                                        .put(5, (entityIn) -> EchoingVoid.doEnderSlash(
                                                                        entityIn,
                                                                        0,
                                                                        entityIn.getYRot() + 270, 0, 0x8000FF, 0,
                                                                        Vec3.ZERO,
                                                                        false, false,
                                                                        EchoingVoid.SECOND_SLASH_DAMAGE,
                                                                        KnockBacks.cancel, 10f,
                                                                        10))
                                                        .put(6, (entityIn) -> EchoingVoid.doEnderSlash(
                                                                        entityIn,
                                                                        0,
                                                                        entityIn.getYRot(), +360, 0x8000FF, 0,
                                                                        Vec3.ZERO,
                                                                        false, false,
                                                                        EchoingVoid.THIRD_SLASH_DAMAGE,
                                                                        KnockBacks.cancel, 10f,
                                                                        10))
                                                        .put(7, (entityIn) -> {
                                                                EchoingVoid.doEnderSlash(
                                                                                entityIn,
                                                                                0,
                                                                                entityIn.getYRot() + 450, 0, 0x8000FF,
                                                                                0,
                                                                                Vec3.ZERO,
                                                                                false, false,
                                                                                EchoingVoid.FOURTH_SLASH_DAMAGE,
                                                                                KnockBacks.cancel, 10f,
                                                                                10);
                                                                EchoingVoid.fallenStarEffect(entityIn);
                                                        })
                                                        .build())::build);

        public static final RegistryObject<ComboState> ECHOING_VOID_END = FD_COMBO_STATES.register("echoing_void_end",
                        ComboState.Builder.newInstance().startAndEnd(743, 764).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(21,
                                                        entity -> SlashBlade.prefix("none")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("echoing_void_end2"))::build);

        public static final RegistryObject<ComboState> ECHOING_VOID_END2 = FD_COMBO_STATES.register("echoing_void_end2",
                        ComboState.Builder.newInstance().startAndEnd(764, 787).priority(50).speed(0.5f)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(ComboState.TimeoutNext.buildFromFrame(21,
                                                        entity -> SlashBlade.prefix("none")))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(0, AttackManager::playQuickSheathSoundAction)
                                                        .put(1, (entityIn) -> EchoingVoid
                                                                        .forceTriggerEchoDamage(entityIn))
                                                        .build())
                                        .releaseAction(ComboState::releaseActionQuickCharge)::build);
        // 寒霜风暴
        public static final RegistryObject<ComboState> FREEZE_ZERO = FD_COMBO_STATES.register("freeze_zero",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> FreezeZero.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("freeze_zero_0")
                                                        : SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> FreezeZero.AntiNTR(entity)
                                                        ? FantasyDesire.prefix("freeze_zero_0")
                                                        : SlashBlade.prefix("none"))::build);

        public static final RegistryObject<ComboState> FREEZE_ZERO_0 = FD_COMBO_STATES.register("freeze_zero_0",
                        ComboState.Builder.newInstance().startAndEnd(1923, 1928).priority(50)
                                        .next(ComboState.TimeoutNext.buildFromFrame(33,
                                                        entity -> FantasyDesire.prefix("freeze_zero_end")))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("freeze_zero_end"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(1, entityIn -> {
                                                                FreezeZero.FreezeZero(entityIn);
                                                                entityIn.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f,
                                                                                1.2f);
                                                        }).build())::build);

        public static final RegistryObject<ComboState> FREEZE_ZERO_END = FD_COMBO_STATES.register("freeze_zero_end",
                        ComboState.Builder.newInstance().startAndEnd(1928, 1963).priority(50)
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(0, AttackManager::playQuickSheathSoundAction).build())
                                        .releaseAction(ComboState::releaseActionQuickCharge)::build);

        public static final RegistryObject<ComboState> FREEZE_ZERO_JUST = FD_COMBO_STATES.register("freeze_zero_just",
                        ComboState.Builder.newInstance().startAndEnd(1923, 1928).priority(45).speed(0.75F)
                                        .next(entity -> FantasyDesire.prefix("freeze_zero_just"))
                                        .nextOfTimeout(entity -> FantasyDesire.prefix("freeze_zero_just_end"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(1, entityIn -> {
                                                                FreezeZero.FreezeZero(entityIn);
                                                                entityIn.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f,
                                                                                1.2f);
                                                        }).build())::build);

        public static final RegistryObject<ComboState> FREEZE_ZERO_JUST_END = FD_COMBO_STATES.register(
                        "freeze_zero_just_end",
                        ComboState.Builder.newInstance().startAndEnd(1928, 1963).priority(50)
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                                                        .put(0, AttackManager::playQuickSheathSoundAction).build())
                                        .releaseAction(ComboState::releaseActionQuickCharge)::build);

        // SmartPistol 模式切换
        public static final RegistryObject<ComboState> CHARGE_SHOT = FD_COMBO_STATES.register("charge_shot",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> tennouboshiuzume.mods.FantasyDesire.slasharts.SmartPistolMode
                                                        .AntiNTR(entity)
                                                                        ? FantasyDesire.prefix("smart_pistol_a_to_b")
                                                                        : SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> tennouboshiuzume.mods.FantasyDesire.slasharts.SmartPistolMode
                                                        .AntiNTR(entity)
                                                                        ? FantasyDesire.prefix("smart_pistol_a_to_b")
                                                                        : SlashBlade.prefix("none"))::build);

        public static final RegistryObject<ComboState> OVER_CHARGE = FD_COMBO_STATES.register("over_charge",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(50)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> tennouboshiuzume.mods.FantasyDesire.slasharts.SmartPistolMode
                                                        .AntiNTR(entity)
                                                                        ? FantasyDesire.prefix("smart_pistol_b_to_a")
                                                                        : SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> tennouboshiuzume.mods.FantasyDesire.slasharts.SmartPistolMode
                                                        .AntiNTR(entity)
                                                                        ? FantasyDesire.prefix("smart_pistol_b_to_a")
                                                                        : SlashBlade.prefix("none"))::build);

        public static final RegistryObject<ComboState> SMART_PISTOL_A_TO_B = FD_COMBO_STATES.register(
                        "smart_pistol_a_to_b",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(80)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .clickAction(entity -> {
                                                ISlashBladeState state = CapabilityUtils
                                                                .getBladeState(entity.getMainHandItem());
                                                IFantasySlashBladeState fdState = CapabilityUtils
                                                                .getFantasyBladeState(entity.getMainHandItem());
                                                SmartPistolMode.dumpAmmo(entity, state,
                                                                fdState);
                                                SmartPistolMode.TransformToB(state,
                                                                fdState);
                                        })::build);

        public static final RegistryObject<ComboState> SMART_PISTOL_B_TO_A = FD_COMBO_STATES.register(
                        "smart_pistol_b_to_a",
                        ComboState.Builder.newInstance().startAndEnd(0, 1).priority(80)
                                        .motionLoc(DefaultResources.ExMotionLocation)
                                        .next(entity -> SlashBlade.prefix("none"))
                                        .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                                        .clickAction(entity -> {
                                                ISlashBladeState state = CapabilityUtils
                                                                .getBladeState(entity.getMainHandItem());
                                                IFantasySlashBladeState fdState = CapabilityUtils
                                                                .getFantasyBladeState(entity.getMainHandItem());
                                                SmartPistolMode.BFGShot(entity, state, fdState);
                                                SmartPistolMode.TransformToA(state,
                                                                fdState);
                                        })::build);
}

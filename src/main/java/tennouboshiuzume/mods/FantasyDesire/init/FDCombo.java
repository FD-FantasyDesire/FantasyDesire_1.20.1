package tennouboshiuzume.mods.FantasyDesire.init;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.event.handler.FallHandler;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AttackHelper;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.TimeValueHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.specialattack.RainbowStar;
import tennouboshiuzume.mods.FantasyDesire.specialattack.TwinSlash;
import tennouboshiuzume.mods.FantasyDesire.specialattack.WingToTheFuture;
import tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.twinblade.TwinBladeEffects;
import tennouboshiuzume.mods.FantasyDesire.specialeffect.globalevent.DelayTaskManager;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

public class FDCombo extends ComboStateRegistry {
    public static final DeferredRegister<ComboState> FD_COMBO_STATES = DeferredRegister.create(ComboState.REGISTRY_KEY,
            FantasyDesire.MODID);
//    TODO：待改造前两个SA。增加限定检测
    public static final RegistryObject<ComboState> WING_TO_THE_FUTURE = FD_COMBO_STATES.register(
            "wing_to_the_future",
            ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(ComboState.TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
                    .nextOfTimeout(entity -> FantasyDesire.prefix("wing_to_the_future_end"))
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(2, (entityIn) -> AttackManager.doSlash(entityIn, -30F, Vec3.ZERO, false, false, 0.1F))
                            .put(9, (entityIn) -> WingToTheFuture.WingToTheFutureElytra(entityIn, entityIn.getMainHandItem())).build())
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
//  虹光星雨
    public static final RegistryObject<ComboState> RAINBOW_STAR = FD_COMBO_STATES.register(
            "rainbow_star",
            ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(ComboState.TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
                    .nextOfTimeout(entity -> FantasyDesire.prefix("rainbow_star_end"))
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(2, (entityIn) -> AttackManager.doSlash(entityIn, -30F, Vec3.ZERO, false, false, 0.1F))
                            .put(3, (entityIn) -> RainbowStar.RainbowStar(entityIn, entityIn.getMainHandItem())).build())
                    .addHitEffect(StunManager::setStun)::build);

    public static final RegistryObject<ComboState> RAINBOW_STAR_END = FD_COMBO_STATES.register("rainbow_star_end", ComboState.Builder
            .newInstance().startAndEnd(459, 488).priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(entity -> SlashBlade.prefix("none"))
            .nextOfTimeout(entity -> SlashBlade.prefix("none"))
            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                    .put(0, AttackManager::playQuickSheathSoundAction).build())
            .releaseAction(ComboState::releaseActionQuickCharge)
            ::build);

//  以下是双刃SA和机制部分
//      双刃 切换形态
    public static final RegistryObject<ComboState> TWIN_MODE = FD_COMBO_STATES.register("twin_mode", ComboState.Builder
            .newInstance().startAndEnd(0, 1).priority(80)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(entity -> SlashBlade.prefix("none"))
            .nextOfTimeout(entity -> SlashBlade.prefix("none"))
//            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
//                .put(0, (entityIn) -> TwinBladeEffects.ConvertForm(entityIn,entityIn.getMainHandItem())).build())
            .clickAction(entity -> TwinBladeEffects.ConvertForm(entity,entity.getMainHandItem()))
            ::build);

//    MOOD 施放前检测
    public static final RegistryObject<ComboState> MOOD_SLASH = FD_COMBO_STATES.register("mood_slash", ComboState.Builder
            .newInstance().startAndEnd(0, 1).priority(80)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(entity -> TwinBladeEffects.AntiNTR(entity)
                    ? FantasyDesire.prefix("mood_slash_0")
                    : FantasyDesire.prefix("twin_mode"))
            .nextOfTimeout(entity -> TwinBladeEffects.AntiNTR(entity)
                    ? FantasyDesire.prefix("mood_slash_0")
                    : FantasyDesire.prefix("twin_mode"))
            ::build);
//    1段，滑步突击
    public static final RegistryObject<ComboState> MOOD_SLASH_0 = FD_COMBO_STATES.register("mood_slash_0", ComboState.Builder
            .newInstance().startAndEnd(1, 33).priority(80)
            .motionLoc(DefaultResources.testLocation)
            .next(ComboState.TimeoutNext.buildFromFrame(31,entity -> FantasyDesire.prefix("mood_slash_1")))
            .nextOfTimeout(entity -> FantasyDesire.prefix("mood_slash_1"))
            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                    .put((int) TimeValueHelper.getTicksFromFrames(30), (entityIn) -> {
                        TwinSlash.RippedStep(entityIn, entityIn.getMainHandItem());
                    })
                    .build())
            ::build);
//    2段，跃升斩
    public static final RegistryObject<ComboState> MOOD_SLASH_1 = FD_COMBO_STATES.register("mood_slash_1", ComboState.Builder
            .newInstance().startAndEnd(1700, 1713).priority(80)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(ComboState.TimeoutNext.buildFromFrame(8,entity -> FantasyDesire.prefix("mood_slash_2")))
            .nextOfTimeout(entity -> FantasyDesire.prefix("mood_slash_2"))
            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                    .put((int) TimeValueHelper.getTicksFromFrames(7),
                            (entityIn) -> {
                                Vec3 motion = entityIn.getDeltaMovement();
                                entityIn.setDeltaMovement(motion.x, 0.6f, motion.z);
                                AttackManager.doSlash(entityIn, -90 + 10, Vec3.ZERO, false, false, 0.50f,
                                        KnockBacks.toss);
                                AttackManager.doSlash(entityIn, -90 - 10, Vec3.ZERO, false, false, 0.50f,
                                        KnockBacks.toss);
                            })
                    .build())
            .addHitEffect((t, a) -> {
                t.setDeltaMovement(0, 0.6f, 0);
                t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
            })
            .addHitEffect((t, a) -> StunManager.setStun(t, 15))::build);
//    3段，双旋斩
    public static final RegistryObject<ComboState> MOOD_SLASH_2 = FD_COMBO_STATES.register("mood_slash_2", ComboState.Builder
            .newInstance().startAndEnd(725, 743).priority(80)
            .next(ComboState.TimeoutNext.buildFromFrame(12,entity -> FantasyDesire.prefix("mood_slash_3")))
            .nextOfTimeout(entity -> FantasyDesire.prefix("mood_slash_3"))
            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                    .put(1, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getMainHandItem(), 0, 0, 45, 72))
                    .put(2, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getMainHandItem(), 0, 0, 45, 0))
                    .put(3, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getMainHandItem(), 0, 0, 45, -72))
                    .put(4, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getMainHandItem(), 0, 0, 45, -72 * 2))
                    .put(5, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getMainHandItem(), 0, 0, 45, -72 * 3))
                    .put(6, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getOffhandItem(), 0, 0, 135, 72))
                    .put(7, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getOffhandItem(), 0, 0, 135, 0))
                    .put(8, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getOffhandItem(), 0, 0, 135, -72))
                    .put(9, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getOffhandItem(), 0, 0, 135, -72 * 2))
                    .put(10, (entityIn) -> TwinSlash.MoodSlash(entityIn, entityIn.getOffhandItem(), 0, 0, 135, -72 * 3))
                    .build())
            .addHitEffect((t, a) -> {
                StunManager.setStun(t, 40);
                TwinSlash.HitEffect(t, 5);
            })
            .addTickAction(FallHandler::fallDecrease)::build);
//    4段，重锤落斩 + 符文引爆
    public static final RegistryObject<ComboState> MOOD_SLASH_3 = FD_COMBO_STATES.register("mood_slash_3", ComboState.Builder
            .newInstance().startAndEnd(500, 576).priority(80)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(entity -> SlashBlade.prefix("none"))
            .nextOfTimeout(entity -> SlashBlade.prefix("none"))
            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                    .put(8, (entityIn) -> {
                        AttackManager.doSlash(entityIn, 90 - 15, false, false, 2.875f);
                        AttackManager.doSlash(entityIn, 90 + 15, true, false, 2.875f);
                        entityIn.moveRelative(entityIn.isInWater() ? 0.35f : 0.8f, new Vec3(0, -0.5, 1.25));
                        TwinBladeEffects.ConvertForm(entityIn,entityIn.getMainHandItem());
                        TwinBladeEffects.ConvertForm(entityIn,entityIn.getOffhandItem());
                    })
                    .build())
            .addTickAction(FallHandler::fallDecrease)
            .addHitEffect((target, attacker) -> {
                // 命中时立即眩晕
                StunManager.setStun(target, 40);
                DelayTaskManager.add(target, 40, () -> {
                    if (target.isAlive() && attacker != null) {
//                        神秘第四币
                        target.playSound(SoundEvents.TRIDENT_THUNDER, 1f, 2f);
                        DamageSource ds = FDDamageSource.entityDamageSource(attacker.level(), FDDamageSource.RESOLUTION, attacker);
                        float damage = (float) AttackHelper.calculateTotalDamage(attacker, target, 4.0f, true);
                        target.hurt(ds, damage);
                        for (int i = 0; i < 4; i++) {
                            Vec3 start = target.position().add(0, target.getBbHeight() / 2, 0);
                            Vec3 end = new Vec3(0, 0, 4);
                            end = end.yRot((float) Math.toRadians(Math.random() * 360f)).xRot((float) Math.toRadians(Math.random() * 360f)).add(start);
                            ParticleUtils.LightBoltParticles(target.level(), start, end, 0x00C8FF, 0.05f, 20, 0.75f, true, 2, 8);
                            ParticleUtils.LightBoltParticles(target.level(), start, end, 0xFF0089, 0.05f, 20, 0.75f, true, 2, 8);
                        }
                    }
                });
            })::build);
//    DOOM 施放前检测
    public static final RegistryObject<ComboState> DOOM_SLASH = FD_COMBO_STATES.register("doom_slash", ComboState.Builder
            .newInstance().startAndEnd(0, 1).priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(entity -> TwinBladeEffects.AntiNTR(entity)
                    ? FantasyDesire.prefix("doom_slash_0")
                    : FantasyDesire.prefix("twin_mode"))
            .nextOfTimeout(entity -> TwinBladeEffects.AntiNTR(entity)
                    ? FantasyDesire.prefix("doom_slash_0")
                    : FantasyDesire.prefix("twin_mode"))
            ::build);
//    1段，闪击
    public static final RegistryObject<ComboState> DOOM_SLASH_0 = FD_COMBO_STATES.register("doom_slash_0", ComboState.Builder
            .newInstance().startAndEnd(1, 33).priority(100)
            .motionLoc(DefaultResources.testLocation)
            .next(ComboState.TimeoutNext.buildFromFrame(32, entity -> FantasyDesire.prefix("doom_slash_1")))
            .nextOfTimeout(entity -> FantasyDesire.prefix("doom_slash_1"))
            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                    .put((int) TimeValueHelper.getTicksFromFrames(30), (entityIn) -> {
                        TwinSlash.DominateStep(entityIn, entityIn.getMainHandItem());
                    })
                    .build())
            ::build);
//    2段，预热
    public static final RegistryObject<ComboState> DOOM_SLASH_1 = FD_COMBO_STATES.register("doom_slash_1",
            ComboState.Builder.newInstance().startAndEnd(700, 720).priority(100)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(ComboState.TimeoutNext.buildFromFrame(13, entity -> FantasyDesire.prefix("doom_slash_2")))
                    .nextOfTimeout(entity -> FantasyDesire.prefix("doom_slash_2"))
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(6, (entityIn) -> {
                                TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -30, 0.244f);
                                TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 180 - 35, 0.244f);
                            })
                            .put(7, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                            .put(8, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                            .put(9, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                            .put(10, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                            .put(11, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                            .put(12, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                            .put(13, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                            .put(14, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                            .build())
                    .addHitEffect(StunManager::setStun)::build);
//    3段，循环狂热
    public static final RegistryObject<ComboState> DOOM_SLASH_2 = FD_COMBO_STATES.register("doom_slash_2", ComboState.Builder
            .newInstance().startAndEnd(710, 720).priority(80)
            .next(ComboState.TimeoutNext.buildFromFrame(3, entity -> FantasyDesire.prefix("doom_slash_3")))
            .nextOfTimeout(entity -> FantasyDesire.prefix("doom_slash_4"))
            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                    .put(0, (entityIn) -> {
                        TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f);
                        TwinSlash.DominateStep(entityIn, entityIn.getMainHandItem());
                        if (entityIn instanceof Player player){
                            player.hurt(player.damageSources().playerAttack(player),2f);
                        }
                    })
                    .put(1, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(2, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(3, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(4, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(5, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(6, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .build())
            .addHitEffect(StunManager::setStun)::build);
//    4段，循环狂热
    public static final RegistryObject<ComboState> DOOM_SLASH_3 = FD_COMBO_STATES.register("doom_slash_3", ComboState.Builder
            .newInstance().startAndEnd(710, 720).priority(80)
            .next(ComboState.TimeoutNext.buildFromFrame(3, entity -> FantasyDesire.prefix("doom_slash_2")))
            .nextOfTimeout(entity -> FantasyDesire.prefix("doom_slash_4"))
            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                    .put(0, (entityIn) -> {
                        TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f);
                        TwinSlash.DominateStep(entityIn, entityIn.getMainHandItem());
                        if (entityIn instanceof Player player){
                            player.hurt(player.damageSources().playerAttack(player),2f);
                        }
                    })
                    .put(1, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(2, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(3, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(4, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(5, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), 90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .put(6, (entityIn) -> TwinSlash.DoomSlash(entityIn, entityIn.getMainHandItem(), -90 + 180 * entityIn.getRandom().nextFloat(), 0.244f))
                    .build())
            .addHitEffect(StunManager::setStun)::build);
//    5段，重锤落，追加群攻
    public static final RegistryObject<ComboState> DOOM_SLASH_4 = FD_COMBO_STATES.register("doom_slash_4", ComboState.Builder
            .newInstance().startAndEnd(500, 576).priority(50)
            .motionLoc(DefaultResources.ExMotionLocation)
            .next(ComboState.TimeoutNext.buildFromFrame(26, entity -> SlashBlade.prefix("none")))
            .nextOfTimeout(entity -> SlashBlade.prefix("none"))
            .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                    .put(8, (entityIn) -> {
                        AttackManager.doSlash(entityIn, 90 - 15, false, false, 2.875f);
                        AttackManager.doSlash(entityIn, 90 + 15, true, false, 2.875f);
                        entityIn.moveRelative(entityIn.isInWater() ? 0.35f : 0.8f, new Vec3(0, -0.5, 5.25));
                        TwinBladeEffects.ConvertForm(entityIn,entityIn.getMainHandItem());
                        TwinBladeEffects.ConvertForm(entityIn,entityIn.getOffhandItem());
                    })
                    .build())
            .addHitEffect((target, attacker) -> {
                        StunManager.setStun(target, 40);
                        if (target.isAlive() && attacker != null) {
                            for (int i = 0; i < 4; i++) {
                                Vec3 start = target.position().add(0, target.getBbHeight() / 2, 0);
                                Vec3 end = new Vec3(0, 0, 10);
                                end = end.yRot((float) Math.toRadians(Math.random() * 360f)).xRot((float) Math.toRadians(Math.random() * 360f)).add(start);
                                ParticleUtils.LightBoltParticles(target.level(), start, end, 0x00C8FF, 0.05f, 20, 0.75f, true, 2, 8);
                                ParticleUtils.LightBoltParticles(target.level(), start, end, 0xFF0089, 0.05f, 20, 0.75f, true, 2, 8);
                            }
                        }
                    }
            )::build);
}


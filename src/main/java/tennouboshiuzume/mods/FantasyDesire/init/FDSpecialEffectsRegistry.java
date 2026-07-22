package tennouboshiuzume.mods.FantasyDesire.init;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.specialeffects.FDSpecialEffectBase;

public class FDSpecialEffectsRegistry {

        public static final DeferredRegister<SpecialEffect> SPECIAL_EFFECT = DeferredRegister
                        .create(SpecialEffect.REGISTRY_KEY, FantasyDesire.MODID);
        // ChikeFlare
        // 消耗耀魂抵挡致死伤害并且永久改变面板
        public static final RegistryObject<SpecialEffect> ImmortalSoul = SPECIAL_EFFECT.register("immortal_soul",
                        () -> new FDSpecialEffectBase(5, false, false, 1));
        // 根据积累的灵魂充能格挡伤害并且触发反击
        public static final RegistryObject<SpecialEffect> SoulShield = SPECIAL_EFFECT.register("soul_shield",
                        () -> new FDSpecialEffectBase(15, false, false, 3));
        // 消耗灵魂充能造成百分比伤害的追加打击
        public static final RegistryObject<SpecialEffect> TyrantStrike = SPECIAL_EFFECT.register("tyrant_strike",
                        () -> new FDSpecialEffectBase(80, false, false, 1));
        // 使该武器相关所有效果不需要前置消耗即可生效
        public static final RegistryObject<SpecialEffect> CheatRumble = SPECIAL_EFFECT.register("cheat_rumble",
                        () -> new FDSpecialEffectBase(800000, false, false, 1));
        // 跨存档认主机制 未实现
        public static final RegistryObject<SpecialEffect> OverDimension = SPECIAL_EFFECT.register("over_dimension",
                        () -> new FDSpecialEffectBase(-1, false, false, 1));
        // Over Cold
        // 配合其特有的进化点数机制改变形态和属性，并且强化SA效果
        public static final RegistryObject<SpecialEffect> EvolutionIce = SPECIAL_EFFECT.register("evolution_ice",
                        () -> new FDSpecialEffectBase(1, false, false, 1));
        // 攻击附加异常并且根据异常效果层数，辅助其叠加进化点数
        public static final RegistryObject<SpecialEffect> ColdLeak = SPECIAL_EFFECT.register("cold_leak",
                        () -> new FDSpecialEffectBase(1, false, false, 1));
        // Pure Snow
        // 特殊合成前置效果 也可以作为SE生效
        // 半通用 非专用刀只有变色效果，专用刀可以随颜色改变攻击属性
        public static final RegistryObject<SpecialEffect> RainbowFlux = SPECIAL_EFFECT.register("rainbow_flux",
                        () -> new FDSpecialEffectBase(1, false, false, 2, true));
        // 幻影剑击中敌人赋予自身 虹羽七刃剑 效果
        public static final RegistryObject<SpecialEffect> PrismFlux = SPECIAL_EFFECT.register("prism_flux",
                        () -> new FDSpecialEffectBase(80, false, false, 1));
        // 击中敌人七次触发幻影剑追加攻击
        public static final RegistryObject<SpecialEffect> ColorFlux = SPECIAL_EFFECT.register("color_flux",
                        () -> new FDSpecialEffectBase(40, false, false, 1));
        // Twin Blade
        // 额外触发一次斩击
        public static final RegistryObject<SpecialEffect> TwinSet = SPECIAL_EFFECT.register("twin_set",
                        () -> new FDSpecialEffectBase(1, false, false, 1, true));
        // Void Transform
        // 特殊合成前置效果
        public static final RegistryObject<SpecialEffect> VoidTransform = SPECIAL_EFFECT.register("void_transform",
                        () -> new FDSpecialEffectBase(1, false, false, 1));
        // Crimson Scythe
        //
        public static final RegistryObject<SpecialEffect> BloodDrain = SPECIAL_EFFECT.register("blood_drain",
                        () -> new FDSpecialEffectBase(60, false, false, 2));
        public static final RegistryObject<SpecialEffect> CrimsonStrike = SPECIAL_EFFECT.register("crimson_strike",
                        () -> new FDSpecialEffectBase(10, false, false, 1));
        // SmartPistol
        public static final RegistryObject<SpecialEffect> EnergyBullet = SPECIAL_EFFECT.register("energy_bullet",
                        () -> new FDSpecialEffectBase(60, false, false, 2));
        public static final RegistryObject<SpecialEffect> TripleBullet = SPECIAL_EFFECT.register("triple_bullet",
                        () -> new FDSpecialEffectBase(40, false, false, 3));
        public static final RegistryObject<SpecialEffect> ThunderBullet = SPECIAL_EFFECT.register("thunder_bullet",
                        () -> new FDSpecialEffectBase(80, true, false, 1));
        public static final RegistryObject<SpecialEffect> ExplosiveBullet = SPECIAL_EFFECT.register("explosive_bullet",
                        () -> new FDSpecialEffectBase(100, true, true, 2));
        // Starless Night
        public static final RegistryObject<SpecialEffect> VoidStrike = SPECIAL_EFFECT.register("void_strike",
                        () -> new FDSpecialEffectBase(100, false, false, 1));
        public static final RegistryObject<SpecialEffect> EchoingStrike = SPECIAL_EFFECT.register("echoing_strike",
                        () -> new FDSpecialEffectBase(30, false, false, 1));

}
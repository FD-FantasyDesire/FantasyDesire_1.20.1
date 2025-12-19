package tennouboshiuzume.mods.FantasyDesire.init;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.specialeffect.FDSpecialEffectBase;

public class FDSpecialEffects {

    public static final DeferredRegister<SpecialEffect> SPECIAL_EFFECT = DeferredRegister.create(SpecialEffect.REGISTRY_KEY, FantasyDesire.MODID);
//    ChikeFlare
    public static final RegistryObject<SpecialEffect> ImmortalSoul = SPECIAL_EFFECT.register("immortal_soul", () -> new FDSpecialEffectBase(5, false, false,1));
    public static final RegistryObject<SpecialEffect> SoulShield = SPECIAL_EFFECT.register("soul_shield", () -> new FDSpecialEffectBase(15, false, false,3));
    public static final RegistryObject<SpecialEffect> TyrantStrike = SPECIAL_EFFECT.register("tyrant_strike", () -> new FDSpecialEffectBase(80, false, false,1));
    public static final RegistryObject<SpecialEffect> CheatRumble = SPECIAL_EFFECT.register("cheat_rumble", () -> new FDSpecialEffectBase(800000, false, false,1));
    public static final RegistryObject<SpecialEffect> OverDimension = SPECIAL_EFFECT.register("over_dimension", () -> new FDSpecialEffectBase(-1, false, false,1));
//    Over Cold
    public static final RegistryObject<SpecialEffect> EvolutionIce = SPECIAL_EFFECT.register("evolution_ice", () -> new FDSpecialEffectBase(1, false, false,1));
    public static final RegistryObject<SpecialEffect> ColdLeak = SPECIAL_EFFECT.register("cold_leak", () -> new FDSpecialEffectBase(1, false, false,1));
//    Pure Snow
    public static final RegistryObject<SpecialEffect> RainbowFlux = SPECIAL_EFFECT.register("rainbow_flux", () -> new FDSpecialEffectBase(1, false, false,2,true));
    public static final RegistryObject<SpecialEffect> PrismFlux = SPECIAL_EFFECT.register("prism_flux", () -> new FDSpecialEffectBase(80, false, false,1));
    public static final RegistryObject<SpecialEffect> ColorFlux = SPECIAL_EFFECT.register("color_flux", () -> new FDSpecialEffectBase(40, false, false,1));
//    Twin Blade
    public static final RegistryObject<SpecialEffect> TwinSet = SPECIAL_EFFECT.register("twin_set",() -> new FDSpecialEffectBase(1, false, false,1,true));
//    Crimson Scythe
    public static final RegistryObject<SpecialEffect> BloodDrain = SPECIAL_EFFECT.register("blood_drain", () -> new FDSpecialEffectBase(60, false, false,1));
    public static final RegistryObject<SpecialEffect> CrimsonStrike = SPECIAL_EFFECT.register("crimson_strike", () -> new FDSpecialEffectBase(10, false, false,1));
//    SmartPistol
    public static final RegistryObject<SpecialEffect> EnergyBullet = SPECIAL_EFFECT.register("energy_bullet", () -> new FDSpecialEffectBase(60, false, false, 2));
    public static final RegistryObject<SpecialEffect> TripleBullet = SPECIAL_EFFECT.register("triple_bullet", () -> new FDSpecialEffectBase(40, false, false, 3));
    public static final RegistryObject<SpecialEffect> ThunderBullet = SPECIAL_EFFECT.register("thunder_bullet", () -> new FDSpecialEffectBase(80, true, false, 1));
    public static final RegistryObject<SpecialEffect> ExplosiveBullet = SPECIAL_EFFECT.register("explosive_bullet", () -> new FDSpecialEffectBase(100, true, true, 2));
//    Starless Night
    public static final RegistryObject<SpecialEffect> VoidStrike = SPECIAL_EFFECT.register("void_strike", () -> new FDSpecialEffectBase(100, false, false,1));
    public static final RegistryObject<SpecialEffect> EchoingStrike = SPECIAL_EFFECT.register("echoing_strike", () -> new FDSpecialEffectBase(30, false, false,1));


}
package tennouboshiuzume.mods.FantasyDesire.data.builtin;

import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.FantasyDefinition;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialAttacks;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;

import java.util.List;

public class FantasySlashBladeBuiltInRegistry {
        public static final ResourceKey<FantasySlashBladeDefinition> ChikeFlare = register("chikeflare");
        public static final ResourceKey<FantasySlashBladeDefinition> SmartPistolA = register("smart_pistol_a");
        public static final ResourceKey<FantasySlashBladeDefinition> SmartPistolB = register("smart_pistol_b");
        public static final ResourceKey<FantasySlashBladeDefinition> CrimsonScythe = register("crimson_scythe");
        public static final ResourceKey<FantasySlashBladeDefinition> TwinBladeL = register("twin_blade_l");
        public static final ResourceKey<FantasySlashBladeDefinition> TwinBladeR = register("twin_blade_r");
        public static final ResourceKey<FantasySlashBladeDefinition> OverColdP0 = register("over_cold_0");
        public static final ResourceKey<FantasySlashBladeDefinition> OverColdP1 = register("over_cold_1");
        public static final ResourceKey<FantasySlashBladeDefinition> OverColdP2 = register("over_cold_2");
        public static final ResourceKey<FantasySlashBladeDefinition> OverColdP3 = register("over_cold_3");
        public static final ResourceKey<FantasySlashBladeDefinition> PureSnow = register("pure_snow");
        public static final ResourceKey<FantasySlashBladeDefinition> ArdorBlossomStar = register("ardor_blossom_star");
        public static final ResourceKey<FantasySlashBladeDefinition> StarlessNight = register("starless_night");
        public static final ResourceKey<FantasySlashBladeDefinition> Crucible = register("crucible");
        public static final ResourceKey<FantasySlashBladeDefinition> GireiKen = register("gireiken");

        public static void registerAll(BootstapContext<FantasySlashBladeDefinition> bootstrap) {
                System.out.println("==== 开始注册数据 ====");
                bootstrap.register(ChikeFlare,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("chikeflare"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0xFFFF00)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/chikeflare.png"))
                                                                .modelName(FantasyDesire
                                                                                .prefix("models/chikeflare.obj"))
                                                                .standbyRenderType(CarryType.PSO2)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(0.2F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(40)
                                                                .addSpecialEffect(FDSpecialEffects.CheatRumble.getId())
                                                                .addSpecialEffect(FDSpecialEffects.TyrantStrike.getId())
                                                                .addSpecialEffect(FDSpecialEffects.SoulShield.getId())
                                                                .addSpecialEffect(FDSpecialEffects.ImmortalSoul.getId())
                                                                .slashArtsType(FDSpecialAttacks.WING_TO_THE_FUTURE
                                                                                .getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .maxSpecialCharge(100)
                                                                .specialChargeName("Soul")
                                                                .specialLore(3)
                                                                .specialAttackLore(6)
                                                                .specialType("Yarimono")
                                                                .specialAttackEffect("dimension")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.UNBREAKING), 10),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.BINDING_CURSE), 1))));
                bootstrap.register(SmartPistolA,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("smart_pistol"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0x00FFFF)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/smartpistol.png"))
                                                                .modelName(FantasyDesire
                                                                                .prefix("models/smartpistol.obj"))
                                                                .standbyRenderType(CarryType.KATANA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(3.0F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(256)
                                                                .addSpecialEffect(FDSpecialEffects.EnergyBullet.getId())
                                                                .addSpecialEffect(FDSpecialEffects.TripleBullet.getId())
                                                                .slashArtsType(FDSpecialAttacks.CHARGE_SHOT.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .maxSpecialCharge(36)
                                                                .specialChargeName("Ammo")
                                                                .specialType("Gunblade")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.POWER_ARROWS), 1),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FALL_PROTECTION), 5),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.UNBREAKING), 5))));

                bootstrap.register(SmartPistolB,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("smart_pistol"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0x99FF00)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/smartpistol_oc.png"))
                                                                .modelName(FantasyDesire
                                                                                .prefix("models/smartpistol.obj"))
                                                                .standbyRenderType(CarryType.KATANA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(3.0F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(256)
                                                                .addSpecialEffect(FDSpecialEffects.EnergyBullet.getId())
                                                                .addSpecialEffect(FDSpecialEffects.TripleBullet.getId())
                                                                .slashArtsType(FDSpecialAttacks.OVER_CHARGE.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .maxSpecialCharge(36)
                                                                .specialChargeName("Ammo")
                                                                .specialType("Gunblade")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.POWER_ARROWS), 1),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FALL_PROTECTION), 5),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.UNBREAKING), 5))));

                bootstrap.register(CrimsonScythe,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("crimson_scythe"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0xFF0000)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/crimsonscythe.png"))
                                                                .modelName(FantasyDesire
                                                                                .prefix("models/crimsonscythe.obj"))
                                                                .standbyRenderType(CarryType.PSO2)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(4.5F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(150)
                                                                .addSpecialEffect(FDSpecialEffects.BloodDrain.getId())
                                                                .addSpecialEffect(
                                                                                FDSpecialEffects.CrimsonStrike.getId())
                                                                .slashArtsType(FDSpecialAttacks.CRIMSON_STRIKE.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .maxSpecialCharge(150)
                                                                .specialType("CrimsonScythe")
                                                                .specialAttackEffect("absorb")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.SHARPNESS), 6),
                                                                new EnchantmentDefinition(
                                                                                getEnchantmentID(Enchantments.THORNS),
                                                                                6),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.MOB_LOOTING), 6),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.UNBREAKING), 6),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FIRE_ASPECT), 6),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FIRE_PROTECTION), 6))));

                bootstrap.register(TwinBladeL,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("twin_blade"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0x00C8FF)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/twinbladeleft.png"))
                                                                .modelName(FantasyDesire.prefix("models/twinblade.obj"))
                                                                .standbyRenderType(CarryType.KATANA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(2.5F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(1024)
                                                                .addSpecialEffect(FDSpecialEffects.TwinSet.getId())
                                                                .slashArtsType(FDSpecialAttacks.TWIN_SYSTEM_L.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .specialType("TwinBladeL")
                                                                .specialAttackEffect("resolution")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.FLAMING_ARROWS), 5),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.BLAST_PROTECTION), 3),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FIRE_PROTECTION), 3),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.INFINITY_ARROWS), 1))));

                bootstrap.register(TwinBladeR,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("twin_blade"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0xFF0089)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/twinbladeright.png"))
                                                                .modelName(FantasyDesire.prefix("models/twinblade.obj"))
                                                                .standbyRenderType(CarryType.KATANA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(2.5F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(1024)
                                                                .addSpecialEffect(FDSpecialEffects.TwinSet.getId())
                                                                .slashArtsType(FDSpecialAttacks.TWIN_SYSTEM_R.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .specialType("TwinBladeR")
                                                                .specialAttackEffect("resolution")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.FLAMING_ARROWS), 5),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.BLAST_PROTECTION), 3),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FIRE_PROTECTION), 3),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.INFINITY_ARROWS), 1))));

                bootstrap.register(OverColdP0,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("over_cold"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0x6699FF)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/overcold.png"))
                                                                .modelName(FantasyDesire
                                                                                .prefix("models/overcold_0.obj"))
                                                                .standbyRenderType(CarryType.RNINJA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(3.2F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(144)
                                                                .addSpecialEffect(FDSpecialEffects.EvolutionIce.getId())
                                                                .addSpecialEffect(FDSpecialEffects.ColdLeak.getId())
                                                                .slashArtsType(FDSpecialAttacks.FREEZE_ZERO.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .specialChargeName("Evolution_0")
                                                                .maxSpecialCharge(300)
                                                                .specialType("OverCold_0")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.FROST_WALKER), 5),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FIRE_PROTECTION), 3),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.UNBREAKING), 3))));
                bootstrap.register(OverColdP1,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("over_cold"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0x6699FF)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/overcold.png"))
                                                                .modelName(FantasyDesire
                                                                                .prefix("models/overcold_1.obj"))
                                                                .standbyRenderType(CarryType.RNINJA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(4.0F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(144)
                                                                .addSpecialEffect(FDSpecialEffects.EvolutionIce.getId())
                                                                .addSpecialEffect(FDSpecialEffects.ColdLeak.getId())
                                                                .slashArtsType(FDSpecialAttacks.FREEZE_ZERO.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .specialChargeName("Evolution_1")
                                                                .maxSpecialCharge(3000)
                                                                .specialType("OverCold_1")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.FROST_WALKER), 5),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FIRE_PROTECTION), 3),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.UNBREAKING), 3))));
                bootstrap.register(OverColdP2,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("over_cold"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0x6699FF)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/overcold.png"))
                                                                .modelName(FantasyDesire
                                                                                .prefix("models/overcold_2.obj"))
                                                                .standbyRenderType(CarryType.RNINJA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(7.2F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(144)
                                                                .addSpecialEffect(FDSpecialEffects.EvolutionIce.getId())
                                                                .addSpecialEffect(FDSpecialEffects.ColdLeak.getId())
                                                                .slashArtsType(FDSpecialAttacks.FREEZE_ZERO.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .specialChargeName("Evolution_2")
                                                                .maxSpecialCharge(30000)
                                                                .specialType("OverCold_2")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.FROST_WALKER), 5),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FIRE_PROTECTION), 3),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.UNBREAKING), 3))));

                bootstrap.register(OverColdP3,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("over_cold"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0x6699FF)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/overcold.png"))
                                                                .modelName(FantasyDesire
                                                                                .prefix("models/overcold_3.obj"))
                                                                .standbyRenderType(CarryType.RNINJA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(13.0F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(144)
                                                                .addSpecialEffect(FDSpecialEffects.EvolutionIce.getId())
                                                                .addSpecialEffect(FDSpecialEffects.ColdLeak.getId())
                                                                .slashArtsType(FDSpecialAttacks.FREEZE_ZERO.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .specialChargeName("Evolution_3")
                                                                .maxSpecialCharge(Integer.MAX_VALUE)
                                                                .specialType("OverCold_3")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.FROST_WALKER), 5),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.FIRE_PROTECTION), 3),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.UNBREAKING), 3))));

                bootstrap.register(PureSnow,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("pure_snow"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0xFFFFFF)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/puresnow.png"))
                                                                .modelName(FantasyDesire.prefix("models/puresnow.obj"))
                                                                .standbyRenderType(CarryType.RNINJA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(2.8F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .addSpecialEffect(FDSpecialEffects.RainbowFlux.getId())
                                                                .addSpecialEffect(FDSpecialEffects.ColorFlux.getId())
                                                                .slashArtsType(FDSpecialAttacks.RAINBOW_STAR.getId())
                                                                .maxDamage(50)
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                .specialType("PureSnow")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.POWER_ARROWS), 1),
                                                                new EnchantmentDefinition(
                                                                                getEnchantmentID(Enchantments.SMITE),
                                                                                1),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.BANE_OF_ARTHROPODS), 1),
                                                                new EnchantmentDefinition(
                                                                                getEnchantmentID(
                                                                                                Enchantments.SHARPNESS),
                                                                                1),
                                                                new EnchantmentDefinition(
                                                                                getEnchantmentID(Enchantments.PIERCING),
                                                                                1),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.BLOCK_FORTUNE), 1),
                                                                new EnchantmentDefinition(
                                                                                getEnchantmentID(Enchantments.IMPALING),
                                                                                1))));

                bootstrap.register(ArdorBlossomStar,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("ardor_blossom_star"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0xFF9900)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/ardorblossomstar.png"))
                                                                .modelName(FantasyDesire
                                                                                .prefix("models/ardorblossomstar.obj"))
                                                                .standbyRenderType(CarryType.KATANA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(3.6F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(58)
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                // .maxSpecialCharge(140)
                                                                // .specialLore(4)
                                                                // .specialEffectLore(6)
                                                                // .specialAttackLore(5)
                                                                .specialType("EGO")
                                                                .build(),
                                                List.of()));

                bootstrap.register(StarlessNight,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("starless_night"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0x8000ff)
                                                                .textureName(FantasyDesire.prefix("models/sn.png"))
                                                                .modelName(FantasyDesire.prefix("models/sn.obj"))
                                                                .standbyRenderType(CarryType.RNINJA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(14.0F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .slashArtsType(FDSpecialAttacks.ECHOING_VOID.getId())
                                                                .maxDamage(1561)
                                                                .addSpecialEffect(FDSpecialEffects.VoidStrike.getId())
                                                                .addSpecialEffect(
                                                                                FDSpecialEffects.EchoingStrike.getId())
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                // .maxSpecialCharge(140)
                                                                // .specialLore(4)
                                                                // .specialEffectLore(6)
                                                                // .specialAttackLore(5)
                                                                .specialType("StarlessNight")
                                                                .specialAttackEffect("echo")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(
                                                                getEnchantmentID(Enchantments.UNBREAKING), 10),
                                                                new EnchantmentDefinition(
                                                                                getEnchantmentID(
                                                                                                Enchantments.SHARPNESS),
                                                                                5),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.POWER_ARROWS), 3),
                                                                new EnchantmentDefinition(
                                                                                getEnchantmentID(Enchantments.MENDING),
                                                                                1))));

                bootstrap.register(Crucible,
                                new FantasySlashBladeDefinition(FantasyDesire.prefix("crucible"),
                                                RenderDefinition.Builder.newInstance()
                                                                .effectColor(0xff0000)
                                                                .textureName(FantasyDesire
                                                                                .prefix("models/crucible.png"))
                                                                .modelName(FantasyDesire.prefix("models/crucible.obj"))
                                                                .standbyRenderType(CarryType.RNINJA)
                                                                .build(),
                                                PropertiesDefinition.Builder.newInstance()
                                                                .baseAttackModifier(14.0F)
                                                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                                                .maxDamage(1561)
                                                                .build(),
                                                FantasyDefinition.Builder.newInstance()
                                                                // .maxSpecialCharge(140)
                                                                // .specialLore(4)
                                                                // .specialEffectLore(6)
                                                                // .specialAttackLore(5)
                                                                .specialType("crucible")
                                                                .specialAttackEffect("eternity")
                                                                .build(),
                                                List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),
                                                                10),
                                                                new EnchantmentDefinition(getEnchantmentID(
                                                                                Enchantments.UNBREAKING), 5),
                                                                new EnchantmentDefinition(
                                                                                getEnchantmentID(Enchantments.MENDING),
                                                                                1))));

                // bootstrap.register(GireiKen,
                // new FantasySlashBladeDefinition(FantasyDesire.prefix("gireiken"),
                // RenderDefinition.Builder.newInstance()
                // .effectColor(0xff0000)
                // .textureName(FantasyDesire
                // .prefix("models/gireiken.png"))
                // .modelName(FantasyDesire.prefix("models/gireiken.obj"))
                // .standbyRenderType(CarryType.RNINJA)
                // .build(),
                // PropertiesDefinition.Builder.newInstance()
                // .baseAttackModifier(14.0F)
                // .defaultSwordType(List.of(SwordType.BEWITCHED))
                // // .slashArtsType(SlashArtsRegistry.SAKURA_BLAST.getId())
                // .maxDamage(1561)
                // .build(),
                // FantasyDefinition.Builder.newInstance()
                // // .maxSpecialCharge(140)
                // // .specialLore(4)
                // // .specialEffectLore(6)
                // // .specialAttackLore(5)
                // .specialType("gireiken")
                // .specialAttackEffect("eternity")
                // .build(),
                // List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.SMITE),
                // 10),
                // new EnchantmentDefinition(getEnchantmentID(
                // Enchantments.UNBREAKING), 5),
                // new EnchantmentDefinition(
                // getEnchantmentID(Enchantments.MENDING),
                // 1))));

                System.out.println("==== 数据注册完成 ====");
        }

        private static ResourceKey<FantasySlashBladeDefinition> register(String id) {
                ResourceKey<FantasySlashBladeDefinition> loc = ResourceKey.create(
                                FantasySlashBladeDefinition.REGISTRY_KEY,
                                FantasyDesire.prefix(id));
                return loc;
        }

        private static ResourceLocation getEnchantmentID(Enchantment enchantment) {
                return ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        }
}

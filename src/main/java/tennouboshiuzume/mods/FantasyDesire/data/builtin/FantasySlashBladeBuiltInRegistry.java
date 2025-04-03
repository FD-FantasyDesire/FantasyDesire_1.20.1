package tennouboshiuzume.mods.FantasyDesire.data.builtin;

import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.FantasyDefinition;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;

import java.util.List;

public class FantasySlashBladeBuiltInRegistry {
    public static final ResourceKey<FantasySlashBladeDefinition> ChikeFlare = register("chikeflare");
    public static final ResourceKey<FantasySlashBladeDefinition> SmartPistol = register("smart_pistol");
    public static final ResourceKey<FantasySlashBladeDefinition> CrimsonScythe = register("crimson_scythe");
    public static final ResourceKey<FantasySlashBladeDefinition> TwinBladeL = register("twin_blade_l");
    public static final ResourceKey<FantasySlashBladeDefinition> TwinBladeR = register("twin_blade_r");
    public static final ResourceKey<FantasySlashBladeDefinition> OverColdP0 = register("over_cold_0");
    public static final ResourceKey<FantasySlashBladeDefinition> OverColdP1 = register("over_cold_1");
    public static final ResourceKey<FantasySlashBladeDefinition> OverColdP2 = register("over_cold_2");
    public static final ResourceKey<FantasySlashBladeDefinition> OverColdP3 = register("over_cold_3");
    public static final ResourceKey<FantasySlashBladeDefinition> PureSnow = register("pure_snow");
    public static final ResourceKey<FantasySlashBladeDefinition> ArdorBlossomStar = register("ardor_blossom_star");
    public static void registerAll(BootstapContext<FantasySlashBladeDefinition> bootstrap) {
        System.out.println("==== 开始注册数据 ====");
        bootstrap.register(ChikeFlare,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("chikeflare"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0xFFFF00)
                                .textureName(FantasyDesire.prefix("models/chikeflare.png"))
                                .modelName(FantasyDesire.prefix("models/chikeflare.obj"))
                                .standbyRenderType(CarryType.PSO2)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(2.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .maxDamage(40)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
                                .maxSpecialCharge(100)
                                .specialLore(3)
                                .specialEffectLore(5)
                                .specialAttackLore(6)
                                .specialType("Yarimono")
                                .specialAttackEffect("Dimension")
                                .build(),
                        List.of()
                )
        );
        bootstrap.register(SmartPistol,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("smart_pistol"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0x00FFFF)
                                .textureName(FantasyDesire.prefix("models/smartpistol.png"))
                                .modelName(FantasyDesire.prefix("models/smartpistol.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(3.0F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.SAKURA_END.getId())
                                .maxDamage(256)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
                                .maxSpecialCharge(21)
//                                .specialLore(4)
//                                .specialEffectLore(6)
//                                .specialAttackLore(5)
                                .specialType("Gunblade")
//                                .specialAttackEffect("ExplosiveShot")
                                .build(),
                        List.of()
                )
        );

        bootstrap.register(CrimsonScythe,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("crimson_scythe"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0xFF0000)
                                .textureName(FantasyDesire.prefix("models/crimsonscythe.png"))
                                .modelName(FantasyDesire.prefix("models/crimsonscythe.obj"))
                                .standbyRenderType(CarryType.PSO2)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(4.5F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.MOONLIGHT.getId())
                                .maxDamage(60)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
                                .maxSpecialCharge(150)
//                                .specialLore(5)
//                                .specialEffectLore(4)
//                                .specialAttackLore(6)
                                .specialType("Reaper")
                                .specialAttackEffect("BloodHarvest")
                                .build(),
                        List.of()
                )
        );

        bootstrap.register(TwinBladeL,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("twin_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0x00C8FF)
                                .textureName(FantasyDesire.prefix("models/twinbladeleft.png"))
                                .modelName(FantasyDesire.prefix("models/twinblade.obj"))
                                .standbyRenderType(CarryType.NINJA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(2.5F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.WIND_CUTTER.getId())
                                .maxDamage(45)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
                                .maxSpecialCharge(110)
//                                .specialLore(3)
//                                .specialEffectLore(4)
//                                .specialAttackLore(5)
                                .specialType("Twin")
//                                .specialAttackEffect("Cyclone")
                                .build(),
                        List.of()
                )
        );

        bootstrap.register(TwinBladeR,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("twin_blade"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0xFF0089)
                                .textureName(FantasyDesire.prefix("models/twinbladeright.png"))
                                .modelName(FantasyDesire.prefix("models/twinblade.obj"))
                                .standbyRenderType(CarryType.RNINJA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(2.5F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.WIND_CUTTER.getId())
                                .maxDamage(45)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
//                                .maxSpecialCharge(110)
//                                .specialLore(3)
//                                .specialEffectLore(4)
//                                .specialAttackLore(5)
                                .specialType("Twin")
//                                .specialAttackEffect("Cyclone")
                                .build(),
                        List.of()
                )
        );

        bootstrap.register(OverColdP0,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("over_cold"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0x6699FF)
                                .textureName(FantasyDesire.prefix("models/overcold.png"))
                                .modelName(FantasyDesire.prefix("models/overcold_0.obj"))
                                .standbyRenderType(CarryType.RNINJA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(3.2F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.FROST_EDGE.getId())
                                .maxDamage(55)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
                                .maxSpecialCharge(130)
//                                .specialLore(4)
//                                .specialEffectLore(5)
//                                .specialAttackLore(4)
                                .specialType("Frost")
                                .specialAttackEffect("Blizzard")
                                .build(),
                        List.of()
                )
        );
        bootstrap.register(OverColdP1,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("over_cold"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0x6699FF)
                                .textureName(FantasyDesire.prefix("models/overcold.png"))
                                .modelName(FantasyDesire.prefix("models/overcold_1.obj"))
                                .standbyRenderType(CarryType.RNINJA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(3.2F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.FROST_EDGE.getId())
                                .maxDamage(55)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
                                .maxSpecialCharge(130)
//                                .specialLore(4)
//                                .specialEffectLore(5)
//                                .specialAttackLore(4)
                                .specialType("Frost")
                                .specialAttackEffect("Blizzard")
                                .build(),
                        List.of()
                )
        );
        bootstrap.register(OverColdP2,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("over_cold"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0x6699FF)
                                .textureName(FantasyDesire.prefix("models/overcold.png"))
                                .modelName(FantasyDesire.prefix("models/overcold_2.obj"))
                                .standbyRenderType(CarryType.RNINJA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(3.2F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.FROST_EDGE.getId())
                                .maxDamage(55)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
                                .maxSpecialCharge(130)
//                                .specialLore(4)
//                                .specialEffectLore(5)
//                                .specialAttackLore(4)
                                .specialType("Frost")
                                .specialAttackEffect("Blizzard")
                                .build(),
                        List.of()
                )
        );

        bootstrap.register(OverColdP3,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("over_cold"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0x6699FF)
                                .textureName(FantasyDesire.prefix("models/overcold.png"))
                                .modelName(FantasyDesire.prefix("models/overcold_3.obj"))
                                .standbyRenderType(CarryType.RNINJA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(3.2F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.FROST_EDGE.getId())
                                .maxDamage(55)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
                                .maxSpecialCharge(130)
                                .specialLore(4)
                                .specialEffectLore(5)
                                .specialAttackLore(4)
                                .specialType("Frost")
                                .specialAttackEffect("Blizzard")
                                .build(),
                        List.of()
                )
        );

        bootstrap.register(PureSnow,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("pure_snow"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0xFFFFFF)
                                .textureName(FantasyDesire.prefix("models/puresnow.png"))
                                .modelName(FantasyDesire.prefix("models/puresnow.obj"))
                                .standbyRenderType(CarryType.RNINJA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(2.8F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.HEAVENLY_STRIKE.getId())
                                .maxDamage(50)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
//                                .maxSpecialCharge(125)
//                                .specialLore(5)
//                                .specialEffectLore(6)
//                                .specialAttackLore(5)
                                .specialType("Miracle")
//                                .specialAttackEffect("DivineLight")
                                .build(),
                        List.of()
                )
        );

        bootstrap.register(ArdorBlossomStar,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("ardor_blossom_star"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(0xFF9900)
                                .textureName(FantasyDesire.prefix("models/ardorblossomstar.png"))
                                .modelName(FantasyDesire.prefix("models/ardorblossomstar.obj"))
                                .standbyRenderType(CarryType.KATANA)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .baseAttackModifier(3.6F)
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
//                                .slashArtsType(SlashArtsRegistry.SAKURA_BLAST.getId())
                                .maxDamage(58)
                                .build(),
                        FantasyDefinition.Builder.newInstance()
//                                .maxSpecialCharge(140)
//                                .specialLore(4)
//                                .specialEffectLore(6)
//                                .specialAttackLore(5)
                                .specialType("EGO")
//                                .specialAttackEffect("Blossom")
                                .build(),
                        List.of()
                )
        );

        System.out.println("==== 数据注册完成 ====");
    }
    private static ResourceKey<FantasySlashBladeDefinition> register(String id) {
        ResourceKey<FantasySlashBladeDefinition> loc = ResourceKey.create(FantasySlashBladeDefinition.REGISTRY_KEY,
                FantasyDesire.prefix(id));
        return loc;
    }

    private static ResourceLocation getEnchantmentID(Enchantment enchantment) {
        return ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
    }
}



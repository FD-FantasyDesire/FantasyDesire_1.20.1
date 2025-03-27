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

    public static void registerAll(BootstapContext<FantasySlashBladeDefinition> bootstrap) {
        System.out.println("==== 开始注册数据 ====");
        bootstrap.register(ChikeFlare,
                new FantasySlashBladeDefinition(FantasyDesire.prefix("chikeflare"),
                        RenderDefinition.Builder.newInstance()
                                .effectColor(-0xFFFF01)
                                .textureName(FantasyDesire.prefix("models/chikeflare.png"))
                                .modelName(FantasyDesire.prefix("models/chikeflare.obj"))
                                .standbyRenderType(CarryType.KATANA)
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



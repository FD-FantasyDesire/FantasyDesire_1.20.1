package tennouboshiuzume.mods.FantasyDesire.data;

import mods.flammpfeil.slashblade.data.builtin.SlashBladeBuiltInRegistry;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.recipe.RequestDefinition;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.Tags.Blocks;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.init.FDSlashArtRegistry;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.recipe.FDRequestDefinition;
import tennouboshiuzume.mods.FantasyDesire.recipe.FantasySlashBladeIngredient;
import tennouboshiuzume.mods.FantasyDesire.recipe.FantasySlashBladeShapedRecipeBuilder;
import tennouboshiuzume.mods.FantasyDesire.recipe.NbtShapedRecipeBuilder;
import tennouboshiuzume.mods.FantasyDesire.recipe.SpecialTransformRecipeBuilder;
import tennouboshiuzume.mods.FantasyDesire.utils.ItemUtils;

import java.util.function.Consumer;

public class FantasySlashBladeRecipeProvider extends RecipeProvider implements IConditionBuilder {

        public FantasySlashBladeRecipeProvider(PackOutput output) {
                super(output);
        }

        @Override
        protected void buildRecipes(@NotNull Consumer<FinishedRecipe> consumer) {
                FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.SmartPistolA.location())
                                .pattern(" EI")
                                .pattern("PBD")
                                .pattern("SI ")
                                .define('B', SlashBladeIngredient.of(
                                                RequestDefinition.Builder
                                                                .newInstance()
                                                                .name(SlashBladeBuiltInRegistry.KOSEKI.location())
                                                                .killCount(1000).build()))
                                .define('I', Ingredient.of(Tags.Items.INGOTS_IRON))
                                .define('S', Ingredient.of(Items.REPEATER))
                                .define('P', Ingredient.of(Items.PAPER))
                                .define('E', Ingredient.of(Items.ENDER_PEARL))
                                .define('D', Ingredient.of(Tags.Items.DYES_LIGHT_BLUE))
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("smart_pistol_a"));

                FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.SmartPistolB.location())
                                .pattern(" EI")
                                .pattern("PBD")
                                .pattern("SI ")
                                .define('B', SlashBladeIngredient.of(
                                                RequestDefinition.Builder
                                                                .newInstance()
                                                                .name(SlashBladeBuiltInRegistry.KOSEKI.location())
                                                                .killCount(1000).build()))
                                .define('I', Ingredient.of(Tags.Items.INGOTS_IRON))
                                .define('S', Ingredient.of(Items.REPEATER))
                                .define('P', Ingredient.of(Items.PAPER))
                                .define('E', Ingredient.of(Items.ENDER_PEARL))
                                .define('D', Ingredient.of(Tags.Items.DYES_LIME))
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("smart_pistol_b"));

                FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.CrimsonScythe.location())
                                .pattern("LLL")
                                .pattern(" OG")
                                .pattern("BG ")
                                .define('B', SlashBladeIngredient.of(
                                                RequestDefinition.Builder
                                                                .newInstance()
                                                                .name(SlashBladeBuiltInRegistry.RUBY
                                                                                .location())
                                                                .killCount(1000)
                                                                .build()))
                                .define('L', Ingredient.of(Items.REDSTONE_BLOCK))
                                .define('G', Ingredient.of(Tags.Items.INGOTS_GOLD))
                                .define('O', Ingredient.of(Tags.Items.OBSIDIAN))
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("crimson_scythe"));

                // 无星之夜 已换成虚无转变制作
                // FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.StarlessNight.location())
                // .pattern(" AO")
                // .pattern("AEA")
                // .pattern("BA ")
                // .define('B', SlashBladeIngredient.of(
                // RequestDefinition.Builder.newInstance()
                // .name(SlashBladeBuiltInRegistry.RODAI_DIAMOND
                // .location())
                // .killCount(500)
                // .addEnchantment(new EnchantmentDefinition(
                // getEnchantmentID(
                // Enchantments.UNBREAKING),
                // 3))
                // .build()))
                // .define('O', Ingredient.of(Items.OBSIDIAN))
                // .define('E', Ingredient.of(Items.ENDER_EYE))
                // .define('A', Ingredient.of(Items.AMETHYST_BLOCK))
                // .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                // has(SlashBladeItems.SLASHBLADE.get()))
                // .save(consumer, FantasyDesire.prefix("starless_night"));

                FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.OverColdP0.location())
                                .pattern(" OO")
                                .pattern("OAO")
                                .pattern("EO ")
                                .define('E', SlashBladeIngredient.of(
                                                RequestDefinition.Builder.newInstance()
                                                                .name(SlashBladeBuiltInRegistry.RODAI_STONE
                                                                                .location())
                                                                .addEnchantment(new EnchantmentDefinition(
                                                                                getEnchantmentID(
                                                                                                Enchantments.FIRE_PROTECTION),
                                                                                3))
                                                                .build()))
                                .define('O', Ingredient.of(Items.ICE))
                                .define('A', Ingredient.of(Items.AMETHYST_BLOCK))
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("over_cold_p0"));

                FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.TwinBladeL.location())
                                .pattern(" AO")
                                .pattern("OEA")
                                .pattern("DO ")
                                .define('E', SlashBladeIngredient.of(
                                                RequestDefinition.Builder.newInstance()
                                                                .name(SlashBladeBuiltInRegistry.RODAI_IRON
                                                                                .location())
                                                                .killCount(906)
                                                                .addEnchantment(new EnchantmentDefinition(
                                                                                getEnchantmentID(
                                                                                                Enchantments.UNBREAKING),
                                                                                3))
                                                                .build()))
                                .define('D', Ingredient.of(Tags.Items.DYES_LIGHT_BLUE))
                                .define('O', Ingredient.of(Items.OBSIDIAN))
                                .define('A', Ingredient.of(Items.AMETHYST_BLOCK))
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("twin_blade_l"));

                FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.TwinBladeR.location())
                                .pattern(" AO")
                                .pattern("OEA")
                                .pattern("DO ")
                                .define('E', SlashBladeIngredient.of(
                                                RequestDefinition.Builder.newInstance()
                                                                .name(SlashBladeBuiltInRegistry.RODAI_IRON
                                                                                .location())
                                                                .killCount(906)
                                                                .addEnchantment(new EnchantmentDefinition(
                                                                                getEnchantmentID(
                                                                                                Enchantments.UNBREAKING),
                                                                                3))
                                                                .build()))
                                .define('D', Ingredient.of(Tags.Items.DYES_MAGENTA))
                                .define('O', Ingredient.of(Items.OBSIDIAN))
                                .define('A', Ingredient.of(Items.AMETHYST_BLOCK))
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("twin_blade_r"));

                // 裁决剑
                FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.Crucible.location())
                                .pattern(" ML")
                                .pattern("MNM")
                                .pattern("BM ")
                                .define('B', SlashBladeIngredient.of(
                                                RequestDefinition.Builder.newInstance()
                                                                .name(SlashBladeBuiltInRegistry.RODAI_NETHERITE
                                                                                .location())
                                                                .killCount(666)
                                                                .proudSoul(6666)
                                                                .build()))
                                .define('M', Ingredient.of(Items.MAGMA_BLOCK))
                                .define('N', Ingredient.of(Items.NETHER_STAR))
                                .define('L', Ingredient.of(Tags.Items.INGOTS_GOLD))
                                .unlockedBy(getHasName(Items.NETHERITE_INGOT), has(Tags.Items.INGOTS_NETHERITE))
                                .save(consumer, FantasyDesire.prefix("crucible"));
                // OverColdP0 -> OverColdP1 特殊转换配方
                SpecialTransformRecipeBuilder.transform(FantasySlashBladeBuiltInRegistry.OverColdP1.location())
                                .addIngredient(FantasySlashBladeIngredient.of(
                                                FDRequestDefinition.Builder.newInstance()
                                                                .name(FantasySlashBladeBuiltInRegistry.OverColdP0
                                                                                .location())
                                                                .specialCharge(300)
                                                                .build()))
                                .addTooltip("jei.fantasydesire.special_transform.overcold_0_to_1")
                                .save(consumer, FantasyDesire.prefix("special_transform_overcold_1"));

                SpecialTransformRecipeBuilder.transform(FantasySlashBladeBuiltInRegistry.OverColdP2.location())
                                .addIngredient(FantasySlashBladeIngredient.of(
                                                FDRequestDefinition.Builder.newInstance()
                                                                .name(FantasySlashBladeBuiltInRegistry.OverColdP1
                                                                                .location())
                                                                .specialCharge(3000)
                                                                .build()))
                                .addTooltip("jei.fantasydesire.special_transform.overcold_1_to_2")
                                .save(consumer, FantasyDesire.prefix("special_transform_overcold_2"));

                SpecialTransformRecipeBuilder.transform(FantasySlashBladeBuiltInRegistry.OverColdP3.location())
                                .addIngredient(FantasySlashBladeIngredient.of(
                                                FDRequestDefinition.Builder.newInstance()
                                                                .name(FantasySlashBladeBuiltInRegistry.OverColdP2
                                                                                .location())
                                                                .specialCharge(30000)
                                                                .build()))
                                .addTooltip("jei.fantasydesire.special_transform.overcold_2_to_3")
                                .save(consumer, FantasyDesire.prefix("special_transform_overcold_3"));

                SpecialTransformRecipeBuilder.transform(FantasySlashBladeBuiltInRegistry.ChikeFlare.location())
                                .addIngredient(SlashBladeIngredient.of(
                                                RequestDefinition.Builder
                                                                .newInstance().build()))
                                .addIngredient(StrictNBTIngredient.of(
                                                ItemUtils.CustomSlashArtSphere(
                                                                new ItemStack(SlashBladeItems.PROUDSOUL_SPHERE.get()),
                                                                FDSlashArtRegistry.WING_TO_THE_FUTURE.get())))
                                .addTooltip("jei.fantasydesire.special_transform.chikeflare")
                                .save(consumer, FantasyDesire.prefix("special_transform_chikeflare"));

                SpecialTransformRecipeBuilder.transform(FantasySlashBladeBuiltInRegistry.StarlessNight.location())
                                .addIngredient(SlashBladeIngredient.of(
                                                RequestDefinition.Builder
                                                                .newInstance()
                                                                .name(SlashBladeBuiltInRegistry.RODAI_NETHERITE
                                                                                .location())
                                                                .proudSoul(5000)
                                                                .killCount(2000)
                                                                .refineCount(5)
                                                                .build()))
                                .addIngredient(StrictNBTIngredient.of(
                                                ItemUtils.CustomEffectShardWithLore(
                                                                new ItemStack(SlashBladeItems.PROUDSOUL_CRYSTAL.get()),
                                                                FDSpecialEffectsRegistry.VoidTransform.get(),
                                                                "jei.fantasydesire.crafting.se.void_transform")))
                                .addTooltip("jei.fantasydesire.special_transform.starless_night")
                                .save(consumer, FantasyDesire.prefix("special_transform_starless_night"));

                SpecialTransformRecipeBuilder.transform(FantasySlashBladeBuiltInRegistry.PureSnow.location())
                                .addIngredient(SlashBladeIngredient.of(
                                                RequestDefinition.Builder
                                                                .newInstance()
                                                                .build()))
                                .addIngredient(StrictNBTIngredient.of(
                                                ItemUtils.CustomEffectShardWithLore(
                                                                new ItemStack(SlashBladeItems.PROUDSOUL_CRYSTAL.get()),
                                                                FDSpecialEffectsRegistry.RainbowFlux.get(),
                                                                "jei.fantasydesire.crafting.se.rainbow_flux")))
                                .addTooltip("jei.fantasydesire.special_transform.pure_snow")
                                .save(consumer, FantasyDesire.prefix("special_transform_pure_snow"));

                NbtShapedRecipeBuilder.nbtShaped(RecipeCategory.COMBAT, ItemUtils.CustomSlashArtSphereWithLore(
                                new ItemStack(SlashBladeItems.PROUDSOUL_SPHERE.get()),
                                FDSlashArtRegistry.WING_TO_THE_FUTURE.get(),
                                "jei.fantasydesire.crafting.sa.wing_to_the_future"))
                                .pattern("PTP")
                                .pattern("YNB")
                                .pattern("PTP")
                                .define('T', Items.TOTEM_OF_UNDYING)
                                .define('N', Items.NETHER_STAR)
                                .define('P', SlashBladeItems.PROUDSOUL.get())
                                .define('B', Items.LIGHT_BLUE_STAINED_GLASS)
                                .define('Y', Items.YELLOW_STAINED_GLASS)
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("sa_wing_to_the_future"));

                NbtShapedRecipeBuilder.nbtShaped(RecipeCategory.COMBAT, ItemUtils.CustomEffectShardWithLore(
                                new ItemStack(SlashBladeItems.PROUDSOUL_CRYSTAL.get()),
                                FDSpecialEffectsRegistry.ThunderBullet.get(),
                                "jei.fantasydesire.crafting.se.thunder_bullet"))
                                .pattern(" TP")
                                .pattern("BCB")
                                .pattern("PT ")
                                .define('C', SlashBladeItems.PROUDSOUL_CRYSTAL.get())
                                .define('T', Items.COPPER_BLOCK)
                                .define('P', SlashBladeItems.PROUDSOUL.get())
                                .define('B', Items.QUARTZ_BLOCK)
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("se_thunder_bullet"));

                NbtShapedRecipeBuilder.nbtShaped(RecipeCategory.COMBAT, ItemUtils.CustomEffectShardWithLore(
                                new ItemStack(SlashBladeItems.PROUDSOUL_CRYSTAL.get()),
                                FDSpecialEffectsRegistry.ExplosiveBullet.get(),
                                "jei.fantasydesire.crafting.se.thunder_bullet"))
                                .pattern(" TP")
                                .pattern("TCT")
                                .pattern("PT ")
                                .define('C', SlashBladeItems.PROUDSOUL_CRYSTAL.get())
                                .define('P', SlashBladeItems.PROUDSOUL.get())
                                .define('T', Items.TNT)
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("se_explosive_bullet"));

                NbtShapedRecipeBuilder.nbtShaped(RecipeCategory.COMBAT, ItemUtils.CustomEffectShardWithLore(
                                new ItemStack(SlashBladeItems.PROUDSOUL_CRYSTAL.get()),
                                FDSpecialEffectsRegistry.VoidTransform.get(),
                                "jei.fantasydesire.crafting.se.void_transform"))
                                .pattern(" TP")
                                .pattern("TCT")
                                .pattern("BT ")
                                .define('C', SlashBladeItems.PROUDSOUL_CRYSTAL.get())
                                .define('T', Items.END_STONE)
                                .define('B', Items.LIGHT_BLUE_DYE)
                                .define('P', Items.PINK_DYE)
                                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()),
                                                has(SlashBladeItems.SLASHBLADE.get()))
                                .save(consumer, FantasyDesire.prefix("se_void_transform"));

        }

        private static ResourceLocation getEnchantmentID(Enchantment enchantment) {
                return ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        }
}

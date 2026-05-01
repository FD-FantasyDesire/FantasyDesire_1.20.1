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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.recipe.FantasySlashBladeShapedRecipeBuilder;

import java.util.function.Consumer;

public class FantasySlashBladeRecipeProvider extends RecipeProvider implements IConditionBuilder {

    private static final ResourceLocation FANTASY_SLASHBLADE = new ResourceLocation(FantasyDesire.MODID, "fantasyslashblade");

    public FantasySlashBladeRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> consumer) {
        FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.SmartPistolA.location())
                .pattern(" EI").pattern("PBD").pattern("SI ")
                .define('B', SlashBladeIngredient.of(SlashBladeItems.SLASHBLADE.get(),
                        RequestDefinition.Builder.newInstance().killCount(1000).build()))
                .define('I', Ingredient.of(Tags.Items.INGOTS_IRON))
                .define('S', Ingredient.of(Tags.Items.STRING))
                .define('P', Ingredient.of(Items.PAPER))
                .define('E', Ingredient.of(Items.ENDER_PEARL))
                .define('D', Ingredient.of(Tags.Items.DYES_BLUE))
                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()), has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer, FantasyDesire.prefix("smart_pistol_a"));

        FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.CrimsonScythe.location())
                .pattern("  L").pattern(" L ").pattern("BG ")
                .define('B', SlashBladeIngredient.of(SlashBladeItems.SLASHBLADE.get(),
                        RequestDefinition.Builder.newInstance().killCount(300).addSwordType(SwordType.BROKEN).build()))
                .define('L', Ingredient.of(Items.REDSTONE_BLOCK))
                .define('G', Ingredient.of(Tags.Items.INGOTS_GOLD))
                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()), has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer, FantasyDesire.prefix("crimson_scythe"));

        FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.TwinBladeL.location())
                .pattern("RLR")
                .pattern("LBL")
                .pattern("RLR")
                .define('B', SlashBladeIngredient.of(RequestDefinition.Builder.newInstance()
                        .name(SlashBladeBuiltInRegistry.RODAI_IRON.location())
                        .killCount(100).addSwordType(SwordType.BROKEN).build()))
                .define('L', Ingredient.of(Items.LIGHT_BLUE_DYE))
                .define('R', Ingredient.of(Items.MAGENTA_DYE))
                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()), has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer, FantasyDesire.prefix("twin_blade_l"));

        FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.TwinBladeR.location())
                .pattern("  L").pattern(" L ").pattern("BG ")
                .define('B', SlashBladeIngredient.of(RequestDefinition.Builder.newInstance()
                        .name(FantasySlashBladeBuiltInRegistry.TwinBladeL.location())
                        .killCount(200).build()))
                .define('L', Ingredient.of(Items.REDSTONE))
                .define('G', Ingredient.of(Tags.Items.INGOTS_GOLD))
                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()), has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer, FantasyDesire.prefix("twin_blade_r"));

//        FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.ArdorBlossomStar.location())
//                .pattern("  L").pattern(" L ").pattern("BG ")
//                .define('B', SlashBladeIngredient.of(SlashBladeItems.SLASHBLADE.get(),
//                        RequestDefinition.Builder.newInstance().killCount(200).addSwordType(SwordType.BROKEN).build()))
//                .define('L', Ingredient.of(Items.FIRE_CHARGE))
//                .define('G', Ingredient.of(Tags.Items.INGOTS_GOLD))
//                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()), has(SlashBladeItems.SLASHBLADE.get()))
//                .save(consumer, FantasyDesire.prefix("ardor_blossom_star"));
//        无星之夜
        FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.StarlessNight.location())
                .pattern(" AO")
                .pattern("AEA")
                .pattern("BA ")
                .define('B', SlashBladeIngredient.of(RequestDefinition.Builder.newInstance()
                        .name(SlashBladeBuiltInRegistry.RODAI_DIAMOND.location())
                        .killCount(500)
                        .addEnchantment(new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING), 3))
                        .build()))
                .define('O', Ingredient.of(Items.OBSIDIAN))
                .define('E', Ingredient.of(Items.ENDER_EYE))
                .define('A', Ingredient.of(Items.AMETHYST_BLOCK))
                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()), has(SlashBladeItems.SLASHBLADE.get()))
                .save(consumer, FantasyDesire.prefix("starless_night"));
//        裁决剑
        FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.Crucible.location())
                .pattern(" ML")
                .pattern("MNM")
                .pattern("BM ")
                .define('B', SlashBladeIngredient.of(RequestDefinition.Builder.newInstance()
                        .name(SlashBladeBuiltInRegistry.RODAI_NETHERITE.location())
                        .killCount(666)
                        .proudSoul(6666)
                        .build()))
                .define('M', Ingredient.of(Items.MAGMA_BLOCK))
                .define('N', Ingredient.of(Items.NETHER_STAR))
                .define('L', Ingredient.of(Tags.Items.INGOTS_GOLD))
                .unlockedBy(getHasName(Items.NETHERITE_INGOT), has(Tags.Items.INGOTS_NETHERITE))
                .save(consumer, FantasyDesire.prefix("crucible"));

//        FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.GireiKen.location())
//                .pattern("  L").pattern(" L ").pattern("BG ")
//                .define('B', SlashBladeIngredient.of(SlashBladeItems.SLASHBLADE.get(),
//                        RequestDefinition.Builder.newInstance().killCount(500).addSwordType(SwordType.BROKEN).build()))
//                .define('L', Ingredient.of(Items.NETHER_STAR))
//                .define('G', Ingredient.of(Tags.Items.INGOTS_GOLD))
//                .unlockedBy(getHasName(SlashBladeItems.SLASHBLADE.get()), has(SlashBladeItems.SLASHBLADE.get()))
//                .save(consumer, FantasyDesire.prefix("gireiken"));
    }


    private static ResourceLocation getEnchantmentID(Enchantment enchantment) {
        return ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
    }
}
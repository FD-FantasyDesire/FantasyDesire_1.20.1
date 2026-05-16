package tennouboshiuzume.mods.FantasyDesire.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.recipe.SpecialTransformRecipe;

public class SpecialTransformCategory implements IRecipeCategory<SpecialTransformRecipe> {

    public static final ResourceLocation UID = FantasyDesire.prefix("special_transform");
    public static final RecipeType<SpecialTransformRecipe> RECIPE_TYPE = RecipeType.create(FantasyDesire.MODID,
            "special_transform", SpecialTransformRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slotDrawable;
    private final Component localizedName;

    public SpecialTransformCategory(IGuiHelper guiHelper) {
        // Minimal background
        this.background = guiHelper.createBlankDrawable(108, 24);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(SlashBladeItems.PROUDSOUL_CRYSTAL.get()));
        this.slotDrawable = guiHelper.getSlotDrawable();
        this.localizedName = Component.translatable("jei.fantasydesire.special_transform");
    }

    @Override
    public RecipeType<SpecialTransformRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return localizedName;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(SpecialTransformRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
            net.minecraft.client.gui.GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Draw slot backgrounds if we're using a blank drawable
        slotDrawable.draw(guiGraphics, 0, 9);
        slotDrawable.draw(guiGraphics, 18, 9);
        slotDrawable.draw(guiGraphics, 36, 9);
        slotDrawable.draw(guiGraphics, 90, 9);
        // Draw an arrow or plus signs if needed, but the prompt just says minimal
        // background, so we just use blank.
        // We can optionally draw the vanilla smithing arrow, but leaving it as simple
        // slots is fine.
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SpecialTransformRecipe recipe, IFocusGroup focuses) {
        // Left slots
        if (recipe.getIngredients().size() > 0) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 10)
                    .addIngredients(recipe.getIngredients().get(0))
                    .setSlotName("Template")
                    .addTooltipCallback((recipeSlotView, tooltip) -> {
                        String key = recipe.getBaseTooltip();
                        if (key != null && !key.isEmpty()) {
                            tooltip.add(Component.translatable(key));
                        }
                    });
        }
        if (recipe.getIngredients().size() > 1) {
            builder.addSlot(RecipeIngredientRole.INPUT, 19, 10)
                    .addIngredients(recipe.getIngredients().get(1))
                    .setSlotName("Base")
                    .addTooltipCallback((recipeSlotView, tooltip) -> {
                        String key = recipe.getAdd1Tooltip();
                        if (key != null && !key.isEmpty()) {
                            tooltip.add(Component.translatable(key));
                        }
                    });
        }
        if (recipe.getIngredients().size() > 2) {
            builder.addSlot(RecipeIngredientRole.INPUT, 37, 10)
                    .addIngredients(recipe.getIngredients().get(2))
                    .setSlotName("Addition")
                    .addTooltipCallback((recipeSlotView, tooltip) -> {
                        String key = recipe.getAdd2Tooltip();
                        if (key != null && !key.isEmpty()) {
                            tooltip.add(Component.translatable(key));
                        }
                    });
        }

        // Right slot (Output)
        net.minecraft.core.RegistryAccess access = net.minecraft.client.Minecraft.getInstance().level != null
                ? net.minecraft.client.Minecraft.getInstance().level.registryAccess()
                : net.minecraft.core.RegistryAccess.EMPTY;

        builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 10)
                .addItemStack(recipe.getResultItem(access))
                .setSlotName("Result")
                .addTooltipCallback((recipeSlotView, tooltip) -> {
                    String key = recipe.getResultTooltip();
                    if (key != null && !key.isEmpty()) {
                        tooltip.add(Component.translatable(key));
                    }
                });
    }
}

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
        this.background = guiHelper.createBlankDrawable(120, 80);
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
        slotDrawable.draw(guiGraphics, 9, 9);
        slotDrawable.draw(guiGraphics, 29, 9);
        slotDrawable.draw(guiGraphics, 89, 9);

        String tooltipKey = recipe.getTooltip();
        if (tooltipKey != null && !tooltipKey.isEmpty()) {
            guiGraphics.drawWordWrap(
                    net.minecraft.client.Minecraft.getInstance().font,
                    Component.translatable(tooltipKey).withStyle(net.minecraft.ChatFormatting.DARK_GRAY),
                    10, 35, 100, 0xFFFFFF);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SpecialTransformRecipe recipe, IFocusGroup focuses) {
        // Left slots
        if (recipe.getIngredients().size() > 0) {
            builder.addSlot(RecipeIngredientRole.INPUT, 10, 10)
                    .addIngredients(recipe.getIngredients().get(0))
                    .setSlotName("Template");
        }
        if (recipe.getIngredients().size() > 1) {
            builder.addSlot(RecipeIngredientRole.INPUT, 30, 10)
                    .addIngredients(recipe.getIngredients().get(1))
                    .setSlotName("Base");
        }

        // Right slot (Output)
        net.minecraft.core.RegistryAccess access = net.minecraft.client.Minecraft.getInstance().level != null
                ? net.minecraft.client.Minecraft.getInstance().level.registryAccess()
                : net.minecraft.core.RegistryAccess.EMPTY;

        builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 10)
                .addItemStack(recipe.getResultItem(access))
                .setSlotName("Result");
    }
}

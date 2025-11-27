package tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.TextNode;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.TextParser;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.TextRenderer;
import tennouboshiuzume.mods.FantasyDesire.specialattack.FDSlashArts;
import tennouboshiuzume.mods.FantasyDesire.specialeffect.FDSpecialEffectBase;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class ItemFantasySlashBlade extends ItemSlashBlade {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager.get(new CapabilityToken<IFantasySlashBladeState>() {
    });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager.get(new CapabilityToken<ISlashBladeState>() {
    });

    public ItemFantasySlashBlade(Tier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, attackSpeedIn, builder);
    }

    @Override
    public Component getName(ItemStack stack) {
        String raw = this.getDescriptionId(stack);
        //富文本解析
        String key = this.getDescriptionId(stack);
        // 从语言系统获取已翻译文本
        String localized = I18n.get(key);
        TextParser parser = new TextParser();
        List<TextNode> roots = parser.parseMultipleTrees(localized);
        long tick = 0;
        if (Minecraft.getInstance().level != null) {
            tick = Minecraft.getInstance().level.getGameTime();
        }
        return TextRenderer.render(roots, tick);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendSwordType(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        EnumSet<SwordType> swordType = SwordType.from(stack);
//        String specialType = stack.getOrCreateTagElement("bladeState").getString("SpecialType");
        LazyOptional<IFantasySlashBladeState> state = stack.getCapability(FDBLADESTATE);
        state.ifPresent((s) -> {
            String specialType = s.getSpecialType();
            if (!specialType.isBlank() && !specialType.equals("Null")) {
                tooltip.add(Component.translatable(String.format("info.fantasydesire." + specialType)));
            } else {
                if (swordType.contains(SwordType.BEWITCHED)) {
                    tooltip.add(Component.translatable("slashblade.sword_type.bewitched").withStyle(ChatFormatting.DARK_PURPLE));
                } else if (swordType.contains(SwordType.ENCHANTED)) {
                    tooltip.add(Component.translatable("slashblade.sword_type.enchanted").withStyle(ChatFormatting.DARK_AQUA));
                } else {
                    tooltip.add(Component.translatable("slashblade.sword_type.noname").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        });
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        stack.getCapability(BLADESTATE).ifPresent((s) -> {
            this.appendSwordType(stack, worldIn, tooltip, flagIn);
            this.appendProudSoulCount(tooltip, stack);
            this.appendKillCount(tooltip, stack);
            this.appendSlashArt(stack, tooltip, s);
            this.appendRefineCount(tooltip, stack);
            this.appendSpecialAttackEffect(tooltip, stack);
            this.appendSpecialEffects(tooltip, s);
            this.appendSpecialLore(tooltip, stack);
            this.appendSASENotice(tooltip, stack);
            this.appendSpecialCharge(tooltip, stack);
        });
    }

    @OnlyIn(Dist.CLIENT)
    public void appendSASENotice(List<Component> tooltip, ItemStack stack) {
        ISlashBladeState state = CapabilityUtils.getBladeState(stack);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(stack);
        if (!Screen.hasShiftDown() && state.getSpecialEffects().stream().anyMatch((se)->FantasyDesire.MODID.equals(se.getNamespace()))) {
            // 提示玩家按 shift 查看更多信息
            tooltip.add(Component.translatable("tooltip.fantasydesire.press_shift_for_details"));
        }
//        System.out.println(state.getSlashArts().getDescription());
        if (!Screen.hasControlDown() && state.getSlashArtsKey()!=null && state.getSlashArtsKey().getNamespace().equals(FantasyDesire.MODID)) {
            // 提示玩家按 Ctrl 查看更多信息
            tooltip.add(Component.translatable("tooltip.fantasydesire.press_ctrl_for_details"));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendSlashArt(ItemStack stack, List<Component> tooltip, @NotNull ISlashBladeState s) {
        EnumSet<SwordType> swordType = SwordType.from(stack);
        ISlashBladeState state = CapabilityUtils.getBladeState(stack);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(stack);
        if (swordType.contains(SwordType.BEWITCHED) && !swordType.contains(SwordType.SEALED)) {
            tooltip.add(Component.translatable("slashblade.tooltip.slash_art", new Object[]{s.getSlashArts().getDescription()}).withStyle(ChatFormatting.GRAY));
        }
        if (Screen.hasControlDown() && state.getSlashArts() instanceof FDSlashArts slashArts && slashArts.getDescColumn()>0) {
            for (int i = 0; i < slashArts.getDescColumn(); i++) {
                tooltip.add(Component.translatable("slash_art.fantasydesire."+ s.getSlashArtsKey().getPath() + ".desc_" + i));
            }
        }

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendSpecialEffects(List<Component> tooltip, @NotNull ISlashBladeState s) {
        if (!s.getSpecialEffects().isEmpty()) {
            Minecraft mcinstance = Minecraft.getInstance();
            Player player = mcinstance.player;
            s.getSpecialEffects().forEach((se) -> {
                boolean showingLevel = SpecialEffect.getRequestLevel(se) > 0;
                tooltip.add(Component.translatable("slashblade.tooltip.special_effect", new Object[]{SpecialEffect.getDescription(se), Component.literal(showingLevel ? String.valueOf(SpecialEffect.getRequestLevel(se)) : "").withStyle(SpecialEffect.isEffective(se, player.experienceLevel) ? ChatFormatting.RED : ChatFormatting.DARK_GRAY)}).withStyle(ChatFormatting.GRAY));
                if (se.getNamespace().equals(FantasyDesire.MODID) && Screen.hasShiftDown() && !Screen.hasControlDown()) {
                    for (int i = 0; i < FDSpecialEffectBase.getDescColumn(se); i++) {
                        tooltip.add(Component.translatable("se.fantasydesire." + se.getPath() + ".desc_" + i));
                    }
                }
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void appendSpecialLore(List<Component> tooltip, ItemStack stack) {
        stack.getCapability(FDBLADESTATE).ifPresent((s) -> {
            int loreCount = s.getSpecialLore(); // 获取需要显示的文本行数
            if (loreCount > 0) {
                stack.getCapability(BLADESTATE).ifPresent((b) -> {
                    String locName = b.getTranslationKey();
                    for (int i = 0; i < loreCount; i++) {
                        Component loreText = Component.translatable(locName + ".desc" + i);
                        tooltip.add(loreText);
                    }
                });
            }
        });
    }

    //    用于显示“特殊充能”
    @OnlyIn(Dist.CLIENT)
    private void appendSpecialCharge(List<Component> tooltip, ItemStack stack) {
        stack.getCapability(FDBLADESTATE).ifPresent((s) -> {
            String scType = s.getSpecialChargeName();
            if (!scType.equals("Null")) {
                int charge = s.getSpecialCharge();
                int maxCharge = s.getMaxSpecialCharge();
                String raw = I18n.get("tooltip.fantasydesire.SpecialCharge." + scType);
                String formatted = String.format(raw, charge, maxCharge); // §颜色保留！
                tooltip.add(Component.literal(formatted));
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private void appendSpecialAttackEffect(List<Component> tooltip, ItemStack stack) {
        stack.getCapability(FDBLADESTATE).ifPresent((s) -> {
            String ae = s.getSpecialAttackEffect();
            if (!ae.equals("Null")) {
//                System.out.println("tooltip.fantasydesire.AttaackEffect."+ae);
                Component attackEffect = Component.translatable("tooltip.fantasydesire.AttackEffect." + ae);
                Component damageText = Component.translatable("tooltip.fantasydesire.AttackEffect", attackEffect);
                tooltip.add(damageText);
                Component attackEffectDesc = Component.translatable("tooltip.fantasydesire.AttackEffect." + ae + ".desc");
                tooltip.add(attackEffectDesc);
            }
        });
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        // 获取父类 Provider（负责 BLADESTATE）
        ICapabilityProvider parentProvider = super.initCapabilities(stack, nbt);
        // 创建子类新增的 Provider（负责 FDBLADESTATE）
        FantasyBladeStateCapabilityProvider fantasyProvider = new FantasyBladeStateCapabilityProvider(stack);
        // 返回组合 Provider
        return new CombinedCapabilityProvider(parentProvider, fantasyProvider);
    }

    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag tag = super.getShareTag(stack);
        stack.getCapability(FDBLADESTATE).ifPresent((state) -> {
            tag.put("fdBladeState", state.serializeNBT());
        });
        return tag;
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
        if (nbt != null && nbt.contains("fdBladeState")) {
            stack.getCapability(FDBLADESTATE).ifPresent((state) -> {
                state.deserializeNBT(nbt.getCompound("fdBladeState"));
            });
        }
        super.readShareTag(stack, nbt);
    }


}

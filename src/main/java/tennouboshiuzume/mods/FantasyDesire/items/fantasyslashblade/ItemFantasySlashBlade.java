package tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
    @OnlyIn(Dist.CLIENT)
    public void appendSwordType(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        EnumSet<SwordType> swordType = SwordType.from(stack);
//        String specialType = stack.getOrCreateTagElement("bladeState").getString("SpecialType");
        LazyOptional<IFantasySlashBladeState> state = stack.getCapability(FDBLADESTATE);
        state.ifPresent((s) -> {
            String specialType = s.getSpecialType();
            if (!specialType.isBlank()){
                tooltip.add(Component.translatable(String.format("info.fantasydesire."+specialType)));
            }else {
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
            this.appendSpecialAttackEffect(tooltip,stack);
            this.appendSpecialEffects(tooltip, s);
            this.appendSpecialLore(tooltip,stack);
            this.appendSpecialAttackLore(tooltip,stack);
            this.appendSpecialEffectLore(tooltip,stack);
            this.appendSpecialCharge(tooltip,stack);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private void appendSpecialLore(List<Component> tooltip, ItemStack stack) {
        stack.getCapability(FDBLADESTATE).ifPresent((s) -> {
            int loreCount = s.getSpecialLore(); // 获取需要显示的文本行数
            if (loreCount>0){
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

    @OnlyIn(Dist.CLIENT)
    private void appendSpecialAttackLore(List<Component> tooltip, ItemStack stack) {
        // 只有按下 Ctrl 键时才显示
        if (Screen.hasControlDown() && !Screen.hasShiftDown()) {
            stack.getCapability(FDBLADESTATE).ifPresent((s) -> {
                int loreCount = s.getSpecialAttackLore(); // 获取需要显示的文本行数
                if (loreCount>0){
                    stack.getCapability(BLADESTATE).ifPresent((b) -> {
                        String locName = b.getTranslationKey();
                        for (int i = 0; i < loreCount; i++) {
                            Component loreText = Component.translatable(locName + ".SpecialAttack.desc" + i);
                            tooltip.add(loreText);
                        }
                    });
                }
            });
        } else {
            // 提示玩家按 Ctrl 查看更多信息
            tooltip.add(Component.translatable("tooltip.fantasydesire.press_ctrl_for_details"));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void appendSpecialEffectLore(List<Component> tooltip, ItemStack stack) {
        // 只有按下 Shift 键时才显示
        if (Screen.hasShiftDown() && !Screen.hasControlDown()) {
            stack.getCapability(FDBLADESTATE).ifPresent((s) -> {
                int loreCount = s.getSpecialEffectLore(); // 获取需要显示的文本行数
                if (loreCount>0){
                    stack.getCapability(BLADESTATE).ifPresent((b) -> {
                        String locName = b.getTranslationKey();
                        for (int i = 0; i < loreCount; i++) {
                            Component loreText = Component.translatable(locName + ".SpecialEffect.desc" + i);
                            tooltip.add(loreText);
                        }
                    });
                }
            });
        } else {
            // 提示玩家按 Ctrl 查看更多信息
            tooltip.add(Component.translatable("tooltip.fantasydesire.press_ctrl_for_details"));
        }
    }
    @OnlyIn(Dist.CLIENT)
    private void appendSpecialCharge(List<Component> tooltip, ItemStack stack){
        stack.getCapability(FDBLADESTATE).ifPresent((s) -> {
            String scType = s.getSpecialChargeName();
            if (!scType.equals("Null")){
                int charge = s.getSpecialCharge();
                int maxCharge = s.getMaxSpecialCharge();
                if (maxCharge>0){
                    Component sc = Component.translatable("tooltip.fantasydesire.SpecialCharge."+scType);
                    Component chargeText = Component.translatable("tooltip.fantasydesire.SpecialCharge", sc, charge, maxCharge);
                    tooltip.add(chargeText);
                }
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private void appendSpecialAttackEffect(List<Component> tooltip, ItemStack stack){
        stack.getCapability(FDBLADESTATE).ifPresent((s) -> {
            String ae = s.getSpecialAttackEffect();
            if (!ae.equals("Null")){
//                System.out.println("tooltip.fantasydesire.AttaackEffect."+ae);
                Component attackEffect = Component.translatable("tooltip.fantasydesire.AttackEffect." + ae);
                Component damageText = Component.translatable("tooltip.fantasydesire.AttackEffect", attackEffect);
                tooltip.add(damageText);
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

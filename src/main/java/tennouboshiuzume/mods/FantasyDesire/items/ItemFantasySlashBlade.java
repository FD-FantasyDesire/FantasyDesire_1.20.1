package tennouboshiuzume.mods.FantasyDesire.items;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.capability.slashblade.NamedBladeStateCapabilityProvider;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class ItemFantasySlashBlade extends ItemSlashBlade {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager.get(new CapabilityToken<IFantasySlashBladeState>() {
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
                tooltip.add(Component.translatable(String.format("tennouboshiuzume.tooltips."+specialType)));
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
            this.appendSpecialEffects(tooltip, s);
            this.appendSpecialCharge(tooltip,stack);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private void appendSpecialCharge(List<Component> tooltip, ItemStack stack){
        stack.getCapability(FDBLADESTATE).ifPresent((s) -> {
            int charge = s.getSpecialCharge();
            int maxCharge = s.getMaxSpecialCharge();
            if (maxCharge>0){
                String chargeRate = String.format("Charge: %s / %s ", charge , maxCharge);
                tooltip.add(Component.literal(ChatFormatting.DARK_RED + chargeRate ));
            }
        });
        stack.getCapability(BLADESTATE).ifPresent((s) -> {
            System.out.println(s.getRefine());
        });
    }

    //    合并Capability
    private static class CombinedCapabilityProvider implements ICapabilityProvider {
        private final ICapabilityProvider[] providers;

        public CombinedCapabilityProvider(ICapabilityProvider... providers) {
            this.providers = providers;
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            for (ICapabilityProvider provider : providers) {
                LazyOptional<T> result = provider.getCapability(cap, side);
                if (result.isPresent()) return result;
            }
            return LazyOptional.empty();
        }
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) {
            return null;
        }
        return new CombinedCapabilityProvider(
                new NamedBladeStateCapabilityProvider(stack), // 原有 Capability
                new FantasyBladeStateCapabilityProvider(stack)    // 自定义 Capability
        );
    }
    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        stack.getCapability(BLADESTATE).ifPresent((state) -> {
            tag.put("bladeState", state.serializeNBT());
        });
        stack.getCapability(FDBLADESTATE).ifPresent((state) -> {
            tag.put("fdBladeState", state.serializeNBT());
        });
        return tag;
    }
    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
        if (nbt != null && nbt.contains("bladeState")) {
            stack.getCapability(BLADESTATE).ifPresent((state) -> {
                state.deserializeNBT(nbt.getCompound("bladeState"));
            });
            stack.getCapability(FDBLADESTATE).ifPresent((state) -> {
                state.deserializeNBT(nbt.getCompound("fdBladeState"));
            });
        }
        super.readShareTag(stack, nbt);
    }

}

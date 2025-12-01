package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;

import java.util.HashMap;
import java.util.Map;

//用于填充创造物品栏和获取物品的工具类
public class ItemUtils {
    public static ItemStack CustomEffectShard(ItemStack stack, SpecialEffect effect) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("SpecialEffectType", effect.toString());
        return stack;
    }
    public static void fillSEShards(CreativeModeTab.Output output){
//        排除本体SE
//        SpecialEffectsRegistry.SPECIAL_EFFECT.getEntries().forEach(specialEffectRegistryObject -> {
//            SpecialEffect se = specialEffectRegistryObject.get();
//            ItemStack sphere = new ItemStack(SBItems.proudsoul_crystal);
//            CompoundTag tag = new CompoundTag();
//            tag.putString("SpecialEffectType", se.toString());
//            sphere.setTag(tag);
//            output.accept(sphere);
//        });
        FDSpecialEffects.SPECIAL_EFFECT.getEntries().forEach(specialEffectRegistryObject -> {
            SpecialEffect se = specialEffectRegistryObject.get();
            ItemStack sphere = new ItemStack(SBItems.proudsoul_crystal);
            CompoundTag tag = new CompoundTag();
            tag.putString("SpecialEffectType", se.toString());
            sphere.setTag(tag);
            output.accept(sphere);
        });
    }

    public static ItemStack dataBakeBlade(ItemStack blade1,ItemStack blade2){
        if (blade1.getItem() instanceof ItemSlashBlade && blade2.getItem() instanceof ItemSlashBlade) {
            ISlashBladeState state1 = CapabilityUtils.getBladeState(blade1);
            ISlashBladeState state2 = CapabilityUtils.getBladeState(blade2);

            // 复制基本属性
            state2.setRefine(state1.getRefine());
            state2.setProudSoulCount(state1.getProudSoulCount());
            state2.setKillCount(state1.getKillCount());

            // 附魔合并（取最大等级）
            Map<Enchantment, Integer> ench1 = EnchantmentHelper.getEnchantments(blade1);
            Map<Enchantment, Integer> ench2 = EnchantmentHelper.getEnchantments(blade2);

            Map<Enchantment, Integer> merged = new HashMap<>(ench2);
            ench1.forEach((ench, lvl) -> merged.merge(ench, lvl, Math::max));

            EnchantmentHelper.setEnchantments(merged, blade2);

            return blade2;
        }
        return blade1;
    }
}

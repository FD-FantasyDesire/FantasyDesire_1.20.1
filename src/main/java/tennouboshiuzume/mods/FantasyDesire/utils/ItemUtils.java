package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.registry.SpecialEffectsRegistry;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;

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
}

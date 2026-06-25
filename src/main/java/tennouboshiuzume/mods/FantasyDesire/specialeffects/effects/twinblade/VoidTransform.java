package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.twinblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.BladeItemEntity;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ItemUtils;

import java.util.Random;

/**
 * 虚空转化效果
 * 当 BladeItemEntity 掉入虚空时，若满足条件则转化为 TwinBladeL 或 TwinBladeR（随机）
 * 条件：击杀数 > 2000, 重铸 > 5, 耀魂 > 5000, 拥有 SE "虚无转变"
 * 转化后出现在世界底部 + 5 格高，发光、漂浮，继承原 blade state 和附魔
 */
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VoidTransform {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onEntityEnterSection(EntityEvent.EnteringSection event) {
        if (!(event.getEntity() instanceof BladeItemEntity bladeItem))
            return;
        ItemStack stack = bladeItem.getItem();
        if (!(stack.getItem() instanceof ItemSlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(stack);
        if (state == null)
            return;
        Level level = bladeItem.level();
        double voidLevel = level.getMinBuildHeight() - 64;
        if (bladeItem.getY() >= voidLevel)
            return;
        if (state.getKillCount() <= 2000)
            return;
        if (state.getRefine() <= 5)
            return;
        if (state.getProudSoulCount() <= 5000)
            return;
        if (!state.hasSpecialEffect(FDSpecialEffectsRegistry.VoidTransform.getId()))
            return;
        ResourceKey<FantasySlashBladeDefinition> targetKey = RANDOM.nextBoolean()
                ? FantasySlashBladeBuiltInRegistry.TwinBladeL
                : FantasySlashBladeBuiltInRegistry.TwinBladeR;
        ItemStack resultBlade = FantasyDesire.getBladeAsRegistry(level, targetKey);
        if (resultBlade.isEmpty())
            return;
        resultBlade = ItemUtils.dataBakeBlade(stack, resultBlade);
        ISlashBladeState resultState = CapabilityUtils.getBladeState(resultBlade);
        if (resultState != null) {
            resultState.removeSpecialEffect(FDSpecialEffectsRegistry.VoidTransform.getId());
        }
        double spawnY = level.getMinBuildHeight() + 5;
        bladeItem.setPos(bladeItem.getX(), spawnY, bladeItem.getZ());
        bladeItem.setItem(resultBlade);
        bladeItem.init();
        bladeItem.setGlowingTag(true);
        bladeItem.setNoGravity(true);
        bladeItem.setOnGround(true);
    }
}

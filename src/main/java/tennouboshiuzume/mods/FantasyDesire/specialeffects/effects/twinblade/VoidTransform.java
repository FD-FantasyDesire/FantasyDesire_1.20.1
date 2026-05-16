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
        // 1. 只处理 BladeItemEntity
        if (!(event.getEntity() instanceof BladeItemEntity bladeItem))
            return;

        // 2. 获取物品栈并检查是否为拔刀剑
        ItemStack stack = bladeItem.getItem();
        if (!(stack.getItem() instanceof ItemSlashBlade))
            return;

        // 3. 获取 blade state
        ISlashBladeState state = CapabilityUtils.getBladeState(stack);
        if (state == null)
            return;

        // 4. 检查是否在虚空中
        // Minecraft 虚空判定: entity.getY() < (level.getMinBuildHeight() - 64)
        Level level = bladeItem.level();
        double voidLevel = level.getMinBuildHeight() - 64;
        if (bladeItem.getY() >= voidLevel)
            return;

        // 5. 检查条件
        // 击杀数 > 2000
        if (state.getKillCount() <= 2000)
            return;
        // 重铸次数 > 5
        if (state.getRefine() <= 5)
            return;
        // 耀魂 > 5000
        if (state.getProudSoulCount() <= 5000)
            return;
        // 拥有 SE void_transform
        if (!state.hasSpecialEffect(FDSpecialEffectsRegistry.VoidTransform.getId()))
            return;

        // 6. 随机选择 TwinBladeL 或 TwinBladeR
        ResourceKey<FantasySlashBladeDefinition> targetKey = RANDOM.nextBoolean()
                ? FantasySlashBladeBuiltInRegistry.TwinBladeL
                : FantasySlashBladeBuiltInRegistry.TwinBladeR;

        // 7. 从注册表中获取目标刀
        ItemStack resultBlade = FantasyDesire.getBladeAsRegistry(level, targetKey);
        if (resultBlade.isEmpty())
            return;

        // 8. 继承原刀的 blade state 和附魔
        // dataBakeBlade 会复制: refine, proudSoul, killCount, 附魔(取最大等级)
        resultBlade = ItemUtils.dataBakeBlade(stack, resultBlade);

        // 9. 从结果刀上移除 void_transform SE，防止转化后的刀再次掉入虚空时无限循环
        ISlashBladeState resultState = CapabilityUtils.getBladeState(resultBlade);
        if (resultState != null) {
            resultState.removeSpecialEffect(FDSpecialEffectsRegistry.VoidTransform.getId());
        }

        // 10. 设置位置：世界底部 + 5
        double spawnY = level.getMinBuildHeight() + 5;
        bladeItem.setPos(bladeItem.getX(), spawnY, bladeItem.getZ());

        // 11. 替换物品栈
        bladeItem.setItem(resultBlade);

        // 12. 重新初始化（设置无敌、生命值等）
        bladeItem.init();

        // 13. 设置发光和漂浮（必须在 init() 之后，否则会被 init() 覆盖）
        bladeItem.setGlowingTag(true);
        bladeItem.setNoGravity(true);
        bladeItem.setOnGround(true);
    }
}

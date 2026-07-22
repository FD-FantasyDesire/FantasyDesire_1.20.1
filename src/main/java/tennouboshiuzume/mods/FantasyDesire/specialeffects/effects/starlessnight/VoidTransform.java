package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.starlessnight;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.data.builtin.SlashBladeBuiltInRegistry;
import mods.flammpfeil.slashblade.entity.BladeItemEntity;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.particle.FlatSpreadingRingParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ItemUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

import java.util.Random;

/**
 * 虚空转化效果
 * 当 BladeItemEntity 掉入虚空时，若满足条件则转化为无星之夜
 * 条件：击杀数 > 2000, 重铸 > 5, 耀魂 > 5000, 拥有 SE "虚无转变"
 * 转化后出现在世界底部 + 5 格高，发光、漂浮，继承原 blade state 和附魔
 */
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VoidTransform {
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
        double voidLevel = level.getMinBuildHeight() - 16;
        if (bladeItem.getY() >= voidLevel
                || state.getKillCount() <= 2000
                || state.getRefine() <= 5
                || state.getProudSoulCount() <= 5000
                || !state.getTranslationKey().equals("item.slashblade.rodai_netherite")
                || !state.hasSpecialEffect(FDSpecialEffectsRegistry.VoidTransform.getId())
                || !level.dimension().equals(Level.END))
            return;
        ResourceKey<FantasySlashBladeDefinition> targetKey = FantasySlashBladeBuiltInRegistry.StarlessNight;
        ItemStack resultBlade = FantasyDesire.getBladeAsRegistry(level, targetKey);
        if (resultBlade.isEmpty())
            return;
        resultBlade = ItemUtils.dataBakeBlade(stack, resultBlade);
        ISlashBladeState resultState = CapabilityUtils.getBladeState(resultBlade);
        if (resultState != null) {
            resultState.removeSpecialEffect(FDSpecialEffectsRegistry.VoidTransform.getId());
        }
        double x = Mth.floor(bladeItem.getX()) + 0.5;
        double z = Mth.floor(bladeItem.getZ()) + 0.5;
        double y = level.getMinBuildHeight() + 5;
        bladeItem.setPos(x, y, z);
        bladeItem.setDeltaMovement(Vec3.ZERO);
        bladeItem.setItem(resultBlade);
        bladeItem.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 8.0F, 1.0F);
        ParticleUtils.AstraLightningParticles(level, bladeItem.position(), new Vec3(x, y + 64, z), 0x5500AA, 0.5f, 40,
                0.8f,
                true, 4,
                16,
                -1);
        if ((level instanceof ServerLevel serverLevel)) {
            for (int i = 0; i < 16; i++) {
                FlatSpreadingRingParticleOptions option = new FlatSpreadingRingParticleOptions(0x5500AA,
                        0.5f + 0.5f * i,
                        0.5f, 20);
                ParticleUtils.sendForceParticles(serverLevel, option, x, y + 4 * i, z, 1, 0, 0, 0, 0, 128.0);
            }
        }
        // 生成一个平台防止剑掉下去，，，
        // 我还以为是没触发合成，搞半天是掉下去了
        BlockPos platformPos = BlockPos.containing(
                x,
                y - 1,
                z);
        if (level.isEmptyBlock(platformPos)) {
            level.setBlock(platformPos, Blocks.PINK_STAINED_GLASS.defaultBlockState(), 3);
        }
    }
}

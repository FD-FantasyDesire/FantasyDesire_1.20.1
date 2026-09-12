package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.puresnow;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.BladeStandEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ItemUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

/** 基于 Forge 原生雷电实体事件的纯净之雪合成机制，不依赖第三方事件 API。 */
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PureSnowLightningEvents {
    private PureSnowLightningEvents() {
    }

    @SubscribeEvent
    public static void onLightningJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LightningBolt lightning))
            return;

        AABB area = lightning.getBoundingBox().inflate(1.0);
        List<BladeStandEntity> stands = event.getLevel().getEntitiesOfClass(BladeStandEntity.class, area);
        for (BladeStandEntity stand : stands) {
            ItemStack blade = stand.getItem();
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            if (!state.hasSpecialEffect(FDSpecialEffectsRegistry.RainbowFlux.getId()))
                continue;
            BlockPos beaconPos = stand.blockPosition().below();
            BlockState blockState = stand.level().getBlockState(beaconPos);
            if (!(blockState.getBlock() instanceof BeaconBlock))
                continue;
            ItemStack targetBlade = FantasyDesire.getBladeAsRegistry(stand.level(),
                    FantasySlashBladeBuiltInRegistry.PureSnow);
            ISlashBladeState targetState = CapabilityUtils.getBladeState(targetBlade);
            if (state.getTranslationKey().equals(targetState.getTranslationKey()))
                continue;
            stand.setItem(ItemUtils.dataBakeBlade(blade, targetBlade));
            for (int i = 0; i < 27; i++) {
                Vec3 base = new Vec3(0, 0, 8);
                Vec3 start = stand.position().add(0, stand.getBbHeight() / 2, 0);
                Vec3 end = base.yRot((float) Math.toRadians(ThreadLocalRandom.current().nextInt(360)))
                        .xRot((float) Math.toRadians(ThreadLocalRandom.current().nextInt(360))).add(start);
                ParticleUtils.LightBoltParticles(stand.level(), start, end,
                        ColorUtils.getSmoothTransitionColor(i, 27, false), 0.1f, 60, 1f, true, 0.8, 8);
            }
            if (stand.level() instanceof ServerLevel serverLevel)
                serverLevel.setWeatherParameters(6000, 0, false, false);
        }
    }
}

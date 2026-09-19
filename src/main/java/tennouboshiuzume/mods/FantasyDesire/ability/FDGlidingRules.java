package tennouboshiuzume.mods.FantasyDesire.ability;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import tennouboshiuzume.mods.FantasyDesire.init.FDSlashArtRegistry;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

/** 额外滑翔来源的公共入口；起飞、移动和状态同步仍由原版负责。 */
public final class FDGlidingRules {
    private static final String CHIKEFLARE_KEY = "item.fantasydesire.chikeflare";

    private FDGlidingRules() {
    }

    public static boolean hasAdditionalGlidingSource(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        ItemStack blade = player.getMainHandItem();
        if (blade.isEmpty() || !(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }

        // 只要求主手刀的身份和专属 SA，不使用附带等级、SE 或折断限制的 SEConditionMatcher。
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        return state != null
                && CHIKEFLARE_KEY.equals(state.getTranslationKey())
                && FDSlashArtRegistry.WING_TO_THE_FUTURE.getId().equals(state.getSlashArtsKey());
    }

    /** 仅在主手能力接管飞行 tick 时调用；flightTicks 为原版累计滑翔 tick 数。 */
    public static void tickAdditionalGliding(LivingEntity entity, int flightTicks) {
        // 保留原版每 10 tick 发出的滑翔游戏事件，供幽匿感测体等机制使用。
        if (!entity.level().isClientSide && (flightTicks + 1) % 10 == 0) {
            entity.gameEvent(GameEvent.ELYTRA_GLIDE);
        }
    }
}

package tennouboshiuzume.mods.FantasyDesire.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.network.FDPlayerVisualStateSync;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 视觉特效状态缓存
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerVisualStateCache {
    private static final Map<UUID, Integer> STATES = new HashMap<>();

    private PlayerVisualStateCache() {
    }

    public static void set(UUID playerId, int flags) {
        STATES.put(playerId, flags);
    }

    public static boolean hasCometState(AbstractClientPlayer player, int flag) {
        if (player == Minecraft.getInstance().player) {
            return false;
        }
        Integer state = STATES.get(player.getUUID());
        return state != null && (state & flag) != 0;
    }

    public static boolean hasFrostStorm(AbstractClientPlayer player) {
        if (player == Minecraft.getInstance().player) {
            return player.hasEffect(FDPotionEffects.FROST_STORM.get());
        }
        Integer state = STATES.get(player.getUUID());
        return state != null && (state & FDPlayerVisualStateSync.FROST_STORM) != 0;
    }

    public static boolean hasCometElytra(AbstractClientPlayer player) {
        if (player == Minecraft.getInstance().player) {
            return player.hasEffect(FDPotionEffects.COMET_ELYTRA.get());
        }
        Integer state = STATES.get(player.getUUID());
        return state != null && (state & FDPlayerVisualStateSync.COMET_ELYTRA) != 0;
    }

    public static void clear() {
        STATES.clear();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}

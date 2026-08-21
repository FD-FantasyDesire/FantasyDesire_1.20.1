package tennouboshiuzume.mods.FantasyDesire.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.PlayerVisualStateCache;

import java.util.UUID;
import java.util.function.Supplier;

public final class FDNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FantasyDesire.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private FDNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, PlayerVisualStateMessage.class,
                PlayerVisualStateMessage::encode,
                PlayerVisualStateMessage::decode,
                PlayerVisualStateMessage::handle);
    }

    public static void sendToTrackingAndSelf(ServerPlayer player, PlayerVisualStateMessage message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
    }

    public static void sendToPlayer(ServerPlayer player, PlayerVisualStateMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public record PlayerVisualStateMessage(UUID playerId, int flags) {
        public static void encode(PlayerVisualStateMessage message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.playerId);
            buffer.writeByte(message.flags);
        }

        public static PlayerVisualStateMessage decode(FriendlyByteBuf buffer) {
            return new PlayerVisualStateMessage(buffer.readUUID(), buffer.readUnsignedByte());
        }

        public static void handle(PlayerVisualStateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> PlayerVisualStateCache.set(message.playerId, message.flags)));
            context.setPacketHandled(true);
        }
    }
}

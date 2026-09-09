package tennouboshiuzume.mods.FantasyDesire.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;

/** 客户端专用的自定义拔刀剑注册表访问入口。 */
public final class FDClientRegistryAccess {
    private FDClientRegistryAccess() {
    }

    public static Registry<FantasySlashBladeDefinition> getRegistry() {
        return Minecraft.getInstance().getConnection().registryAccess()
                .registryOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY);
    }
}

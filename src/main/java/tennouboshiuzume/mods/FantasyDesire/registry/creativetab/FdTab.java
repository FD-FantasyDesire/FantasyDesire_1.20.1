package tennouboshiuzume.mods.FantasyDesire.registry.creativetab;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.registry.ModItems;

public class FdTab {
    // 创建一个 `DeferredRegister` 用于注册创造模式物品栏
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FantasyDesire.MODID);

    // 注册 `Fantasy Desire` 物品栏
    public static final RegistryObject<CreativeModeTab> FANTASY_TAB = CREATIVE_MODE_TABS.register("fantasy_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fantasydesire")) // 物品栏名称
                    .icon(() -> new ItemStack(ModItems.CUSTOM_SLASHBLADE.get())) // 物品栏图标
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CUSTOM_SLASHBLADE.get()); // 添加物品
                    })
                    .build());

    // 方法用于注册物品栏
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

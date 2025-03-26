package tennouboshiuzume.mods.FantasyDesire.registry.creativetab;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.SpecialEffectsRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.registry.ModItems;
import tennouboshiuzume.mods.FantasyDesire.utils.ItemUtils;

public class FdTab {
    // 创建一个 `DeferredRegister` 用于注册创造模式物品栏
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FantasyDesire.MODID);

    // 注册 `Fantasy Desire` 物品栏
    public static final RegistryObject<CreativeModeTab> FANTASY_TAB = CREATIVE_MODE_TABS.register("fantasy_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fantasydesire")).icon(() -> {
                        ItemStack stack = new ItemStack(SBItems.slashblade);
                        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(s -> {
                            s.setModel(new ResourceLocation(SlashBlade.MODID, "model/named/yamato.obj"));
                            s.setTexture(new ResourceLocation(SlashBlade.MODID, "model/named/yamato.png"));
                        });
                        return stack;
                    }).displayItems((parameters, output) -> {
//                        output.accept(ItemUtils.CustomEffectShard(new ItemStack(SBItems.proudsoul_crystal), SpecialEffectsRegistry.WITHER_EDGE.get())); // 添加物品
                        ItemUtils.fillSEShards(output);
                    })
                    .build());

    // 方法用于注册物品栏
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

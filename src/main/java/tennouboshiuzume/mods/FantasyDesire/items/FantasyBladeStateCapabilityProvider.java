//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package tennouboshiuzume.mods.FantasyDesire.items;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class FantasyBladeStateCapabilityProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
    protected LazyOptional<IFantasySlashBladeState> state;
    private ItemStack blade;

    public FantasyBladeStateCapabilityProvider(ItemStack blade) {
        this.state = LazyOptional.of(() -> {
            return new FantasySlashBladeState(blade);
        });
        this.blade = blade;
    }

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return ItemFantasySlashBlade.FDBLADESTATE.orEmpty(cap, this.state);
    }

    public CompoundTag serializeNBT() {
        return ((IFantasySlashBladeState)this.state.orElseGet(() -> {
            return new FantasySlashBladeState(this.blade);
        })).serializeNBT();
    }

    public void deserializeNBT(CompoundTag inTag) {
        this.state.ifPresent((instance) -> {
            instance.deserializeNBT(inTag);
        });
    }
}

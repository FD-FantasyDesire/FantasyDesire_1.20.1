package tennouboshiuzume.mods.FantasyDesire.items;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class CombinedCapabilityProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

    private final ICapabilityProvider parentProvider;
    private final FantasyBladeStateCapabilityProvider fantasyProvider;

    public CombinedCapabilityProvider(ICapabilityProvider parentProvider, FantasyBladeStateCapabilityProvider fantasyProvider) {
        this.parentProvider = parentProvider;
        this.fantasyProvider = fantasyProvider;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        // 如果请求的是子类新增的 Capability，则返回 fantasyProvider
        if (cap == ItemFantasySlashBlade.FDBLADESTATE) {
            return fantasyProvider.getCapability(cap, side);
        }
        // 如果请求的是父类的 Capability，则返回 parentProvider
        if (cap == ItemSlashBlade.BLADESTATE) {
            return parentProvider != null ? parentProvider.getCapability(cap, side) : LazyOptional.empty();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        // 保存父类的数据到 "bladeState" 键中
        if (parentProvider instanceof ICapabilitySerializable) {
            ICapabilitySerializable<CompoundTag> ser = (ICapabilitySerializable<CompoundTag>) parentProvider;
            tag.put("bladeState", ser.serializeNBT());
        }
        // 保存子类新增的数据到 "fdBladeState" 键中
        if (fantasyProvider instanceof ICapabilitySerializable) {
            ICapabilitySerializable<CompoundTag> ser = (ICapabilitySerializable<CompoundTag>) fantasyProvider;
            tag.put("fdBladeState", ser.serializeNBT());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        // 反序列化父类数据
        if (nbt.contains("bladeState") && parentProvider instanceof ICapabilitySerializable) {
            ICapabilitySerializable<CompoundTag> ser = (ICapabilitySerializable<CompoundTag>) parentProvider;
            ser.deserializeNBT(nbt.getCompound("bladeState"));
        }
        // 反序列化子类新增的数据
        if (nbt.contains("fdBladeState") && fantasyProvider instanceof ICapabilitySerializable) {
            ICapabilitySerializable<CompoundTag> ser = (ICapabilitySerializable<CompoundTag>) fantasyProvider;
            ser.deserializeNBT(nbt.getCompound("fdBladeState"));
        }
    }
}

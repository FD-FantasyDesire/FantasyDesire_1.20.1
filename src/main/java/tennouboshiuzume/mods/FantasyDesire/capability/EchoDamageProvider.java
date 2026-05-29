package tennouboshiuzume.mods.FantasyDesire.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class EchoDamageProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IEchoDamageCap> ECHO_DAMAGE = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final IEchoDamageCap instance = new EchoDamageCap();
    private final LazyOptional<IEchoDamageCap> optional = LazyOptional.of(() -> instance);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ECHO_DAMAGE) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<UUID, Float> entry : instance.getAllDamage().entrySet()) {
            tag.putFloat(entry.getKey().toString(), entry.getValue());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.clearDamage();
        for (String key : nbt.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                instance.addDamage(uuid, nbt.getFloat(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
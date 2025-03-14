package tennouboshiuzume.mods.FantasyDesire.items;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IFantasySlashBladeState extends INBTSerializable<CompoundTag> {
    default CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SpecialCharge", this.getSpecialCharge());
        tag.putInt("MaxSpecialCharge", this.getMaxSpecialCharge());
        tag.putInt("SpecialEffectLore", this.getSpecialEffectLore());
        tag.putInt("SpecialAttackLore", this.getSpecialAttackLore());
        tag.putInt("SpecialLore", this.getSpecialLore());
        tag.putString("SpecialType", this.getSpecialType());
        return tag;
    }
    default void deserializeNBT(CompoundTag tag) {
        if (tag != null) {
            this.setSpecialCharge(tag.getInt("SpecialCharge"));
            this.setMaxSpecialCharge(tag.getInt("MaxSpecialCharge"));
            this.setSpecialEffectLore(tag.getInt("SpecialEffectLore"));
            this.setSpecialAttackLore(tag.getInt("SpecialAttackLore"));
            this.setSpecialLore(tag.getInt("SpecialLore"));
            this.setSpecialType(tag.getString("SpecialType"));
        }
    }
    // Getter methods
    int getSpecialCharge();
    int getMaxSpecialCharge();
    int getSpecialLore();
    int getSpecialAttackLore();
    int getSpecialEffectLore();
    String getSpecialType();
    // Setter methods
    void setSpecialCharge(int specialCharge);
    void setMaxSpecialCharge(int specialCharge);
    void setSpecialLore(int specialLore);
    void setSpecialAttackLore(int specialAttackLore);
    void setSpecialEffectLore(int specialEffectLore);
    void setSpecialType(String specialType);
}

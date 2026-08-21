package tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IFantasySlashBladeState extends INBTSerializable<CompoundTag> {
    default CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SpecialCharge", this.getSpecialCharge());
        tag.putInt("MaxSpecialCharge", this.getMaxSpecialCharge());
        tag.putString("SpecialType", this.getSpecialType());
        tag.putString("SpecialChargeName", this.getSpecialChargeName());
        tag.putString("SpecialAttackEffect",this.getSpecialAttackEffect());
        return tag;
    }


    default void deserializeNBT(CompoundTag tag) {
        if (tag != null) {
            this.setSpecialCharge(tag.getInt("SpecialCharge"));
            this.setMaxSpecialCharge(tag.getInt("MaxSpecialCharge"));
            this.setSpecialType(tag.getString("SpecialType"));
            this.setSpecialChargeName(tag.getString("SpecialChargeName"));
            this.setSpecialAttackEffect(tag.getString("SpecialAttackEffect"));
        }
    }
    // Getter methods
    int getSpecialCharge();
    int getMaxSpecialCharge();
    String getSpecialType();
    String getSpecialChargeName();
    String getSpecialAttackEffect();
    // Setter methods
    void setSpecialCharge(int specialCharge);
    void setMaxSpecialCharge(int specialCharge);
    void setSpecialType(String specialType);
    void setSpecialChargeName(String specialChargeName);
    void setSpecialAttackEffect(String specialAttackEffect);
}

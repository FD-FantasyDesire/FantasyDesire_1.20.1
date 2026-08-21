
package tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade;

import net.minecraft.world.item.ItemStack;


public class FantasySlashBladeState implements IFantasySlashBladeState {
    protected int SpecialCharge = 0;
    protected int MaxSpecialCharge = 0;
    protected String SpecialType = "Null";
    protected String SpecialChargeName = "Null";
    protected String SpecialAttackEffect = "Null";

    public FantasySlashBladeState(ItemStack blade) {
        if (!blade.isEmpty() && blade.getOrCreateTag().contains("fdBladeState")) {
            this.deserializeNBT(blade.getTagElement("fdBladeState"));
        }
    }

    public int getSpecialCharge() {
        return this.SpecialCharge;
    }

    public int getMaxSpecialCharge() {
        return this.MaxSpecialCharge;
    }

    public String getSpecialType() {
        return this.SpecialType;
    }

    public String getSpecialChargeName() {
        return this.SpecialChargeName;
    }

    public String getSpecialAttackEffect() {
        return SpecialAttackEffect;
    }

    public void setSpecialCharge(int specialCharge) {
        this.SpecialCharge = specialCharge;
    }

    public void setMaxSpecialCharge(int maxSpecialCharge) {
        this.MaxSpecialCharge = maxSpecialCharge;
    }

    public void setSpecialType(String specialType) {
        this.SpecialType = specialType;
    }

    public void setSpecialChargeName(String specialChargeName) {
        this.SpecialChargeName = specialChargeName;
    }

    public void setSpecialAttackEffect(String specialAttackEffect) {
        this.SpecialAttackEffect = specialAttackEffect;
    }

}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package tennouboshiuzume.mods.FantasyDesire.data;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.resources.ResourceLocation;

public class FantasyDefinition {
    public static final Codec<FantasyDefinition> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(
                Codec.INT.optionalFieldOf("special_charge", 0).forGetter(FantasyDefinition::getSpecialCharge),
                Codec.INT.optionalFieldOf("max_special_charge", 0).forGetter(FantasyDefinition::getMaxSpecialCharge),
                Codec.INT.optionalFieldOf("special_lore", 0).forGetter(FantasyDefinition::getSpecialLore),
                Codec.INT.optionalFieldOf("special_effect_lore", 0).forGetter(FantasyDefinition::getSpecialEffectLore),
                Codec.INT.optionalFieldOf("special_attack_lore", 0).forGetter(FantasyDefinition::getSpecialAttackLore),
                Codec.STRING.optionalFieldOf("special_type", "Fantasy").forGetter(FantasyDefinition::getSpecialType)
        ).apply(instance, FantasyDefinition::new);
    });
    private final int SpecialCharge;
    private final int MaxSpecialCharge;
    private final int SpecialLore ;
    private final int SpecialEffectLore;
    private final int SpecialAttackLore;
    private final String SpecialType;

    private FantasyDefinition(int specialCharge,int maxSpecialCharge,int specialLore,int specialEffectLore,int specialAttackLore,String specialType) {
        this.SpecialCharge = specialCharge;
        this.MaxSpecialCharge = maxSpecialCharge;
        this.SpecialLore = specialLore;
        this.SpecialEffectLore = specialEffectLore;
        this.SpecialAttackLore = specialAttackLore;
        this.SpecialType = specialType;
    }
    public int getSpecialCharge() {
        return SpecialCharge;
    }

    public int getMaxSpecialCharge() {
        return MaxSpecialCharge;
    }

    public int getSpecialLore() {
        return SpecialLore;
    }

    public int getSpecialEffectLore() {
        return SpecialEffectLore;
    }

    public int getSpecialAttackLore() {
        return SpecialAttackLore;
    }

    public String getSpecialType() {
        return SpecialType;
    }

    public static class Builder {
        private int specialCharge;
        private int maxSpecialCharge;
        private int specialLore;
        private int specialEffectLore;
        private int specialAttackLore;
        private String specialType;

        private Builder() {
            this.specialCharge = 0;
            this.maxSpecialCharge = 0;
            this.specialLore = 0;
            this.specialEffectLore = 0;
            this.specialAttackLore = 0;
            this.specialType = "Fantasy";
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder specialCharge(int specialCharge) {
            this.specialCharge = specialCharge;
            return this;
        }

        public Builder maxSpecialCharge(int maxSpecialCharge) {
            this.maxSpecialCharge = maxSpecialCharge;
            return this;
        }

        public Builder specialLore(int specialLore) {
            this.specialLore = specialLore;
            return this;
        }

        public Builder specialEffectLore(int specialEffectLore) {
            this.specialEffectLore = specialEffectLore;
            return this;
        }

        public Builder specialAttackLore(int specialAttackLore) {
            this.specialAttackLore = specialAttackLore;
            return this;
        }

        public Builder specialType(String specialType) {
            this.specialType = specialType;
            return this;
        }

        public FantasyDefinition build() {
            return new FantasyDefinition(this.specialCharge, this.maxSpecialCharge, this.specialLore, this.specialEffectLore, this.specialAttackLore, this.specialType);
        }
    }
}

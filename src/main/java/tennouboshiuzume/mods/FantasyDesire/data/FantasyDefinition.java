package tennouboshiuzume.mods.FantasyDesire.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class FantasyDefinition {
    public static final Codec<FantasyDefinition> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(
                Codec.INT.optionalFieldOf("special_charge", 0).forGetter(FantasyDefinition::getSpecialCharge),
                Codec.INT.optionalFieldOf("max_special_charge", 0).forGetter(FantasyDefinition::getMaxSpecialCharge),
                Codec.INT.optionalFieldOf("special_lore", 0).forGetter(FantasyDefinition::getSpecialLore),
                Codec.INT.optionalFieldOf("special_effect_lore", 0).forGetter(FantasyDefinition::getSpecialEffectLore),
                Codec.INT.optionalFieldOf("special_attack_lore", 0).forGetter(FantasyDefinition::getSpecialAttackLore),
                Codec.STRING.optionalFieldOf("special_type", "Null").forGetter(FantasyDefinition::getSpecialType),
                Codec.STRING.optionalFieldOf("special_charge_name", "Null")
                        .forGetter(FantasyDefinition::getSpecialChargeName),
                Codec.STRING.optionalFieldOf("special_attack_effect", "Null")
                        .forGetter(FantasyDefinition::getSpecialAttackEffect))
                .apply(instance, FantasyDefinition::new);
    });
    private final int specialCharge;
    private final int maxSpecialCharge;
    private final int specialLore;
    private final int specialEffectLore;
    private final int specialAttackLore;
    private final String specialType;
    private final String specialChargeName;
    private final String specialAttackEffect;

    private FantasyDefinition(int specialCharge, int maxSpecialCharge, int specialLore, int specialEffectLore,
            int specialAttackLore, String specialType, String specialChargeName, String specialAttackEffect) {
        this.specialCharge = specialCharge;
        this.maxSpecialCharge = maxSpecialCharge;
        this.specialLore = specialLore;
        this.specialEffectLore = specialEffectLore;
        this.specialAttackLore = specialAttackLore;
        this.specialType = specialType;
        this.specialChargeName = specialChargeName;
        this.specialAttackEffect = specialAttackEffect;
    }

    public int getSpecialCharge() {
        return specialCharge;
    }

    public int getMaxSpecialCharge() {
        return maxSpecialCharge;
    }

    public int getSpecialLore() {
        return specialLore;
    }

    public int getSpecialEffectLore() {
        return specialEffectLore;
    }

    public int getSpecialAttackLore() {
        return specialAttackLore;
    }

    public String getSpecialType() {
        return specialType;
    }

    public String getSpecialChargeName() {
        return specialChargeName;
    }

    public String getSpecialAttackEffect() {
        return specialAttackEffect;
    }

    public static class Builder {
        private int specialCharge;
        private int maxSpecialCharge;
        private int specialLore;
        private int specialEffectLore;
        private int specialAttackLore;
        private String specialType;
        private String specialChargeName;
        private String specialAttackEffect;

        private Builder() {
            this.specialCharge = 0;
            this.maxSpecialCharge = 0;
            this.specialLore = 0;
            this.specialEffectLore = 0;
            this.specialAttackLore = 0;
            this.specialType = "Null";
            this.specialChargeName = "Null";
            this.specialAttackEffect = "Null";
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

        public Builder specialChargeName(String specialChargeName) {
            this.specialChargeName = specialChargeName;
            return this;
        }

        public Builder specialAttackEffect(String specialAttackEffect) {
            this.specialAttackEffect = specialAttackEffect;
            return this;
        }

        public FantasyDefinition build() {
            return new FantasyDefinition(this.specialCharge, this.maxSpecialCharge, this.specialLore,
                    this.specialEffectLore, this.specialAttackLore, this.specialType, this.specialChargeName,
                    this.specialAttackEffect);
        }
    }
}

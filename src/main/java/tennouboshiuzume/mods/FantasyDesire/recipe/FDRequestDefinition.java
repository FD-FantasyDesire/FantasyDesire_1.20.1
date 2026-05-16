package tennouboshiuzume.mods.FantasyDesire.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tennouboshiuzume.mods.FantasyDesire.data.FantasyDefinition;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.FantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record FDRequestDefinition(ResourceLocation name, int proudSoulCount, int killCount, int refineCount,
        List<EnchantmentDefinition> enchantments, List<SwordType> defaultType,
        FantasyDefinition fantasyDefinition) {

    public static final Codec<FDRequestDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("name", SlashBlade.prefix("none"))
                    .forGetter(FDRequestDefinition::name),
            Codec.INT.optionalFieldOf("proud_soul", 0).forGetter(FDRequestDefinition::proudSoulCount),
            Codec.INT.optionalFieldOf("kill", 0).forGetter(FDRequestDefinition::killCount),
            Codec.INT.optionalFieldOf("refine", 0).forGetter(FDRequestDefinition::refineCount),
            EnchantmentDefinition.CODEC.listOf().optionalFieldOf("enchantments", Lists.newArrayList())
                    .forGetter(FDRequestDefinition::enchantments),
            SwordType.CODEC.listOf().optionalFieldOf("sword_type", Lists.newArrayList())
                    .forGetter(FDRequestDefinition::defaultType),
            FantasyDefinition.CODEC.optionalFieldOf("fantasy", FantasyDefinition.Builder.newInstance().build())
                    .forGetter(FDRequestDefinition::fantasyDefinition))
            .apply(instance, FDRequestDefinition::new));

    public static FDRequestDefinition fromJSON(JsonObject json) {
        return CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(msg -> SlashBlade.LOGGER.error("Failed to parse : {}", msg))
                .orElseGet(Builder.newInstance()::build);
    }

    public JsonElement toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this)
                .resultOrPartial(msg -> SlashBlade.LOGGER.error("Failed to encode : {}", msg)).orElseThrow();
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.name());
        buffer.writeInt(this.proudSoulCount());
        buffer.writeInt(this.killCount());
        buffer.writeInt(this.refineCount());
        buffer.writeCollection(this.enchantments(), (buf, request) -> {
            buf.writeResourceLocation(request.getEnchantmentID());
            buf.writeByte(request.getEnchantmentLevel());
        });
        buffer.writeCollection(this.defaultType(), (buf, request) -> buf.writeUtf(request.name().toLowerCase()));

        FantasyDefinition.CODEC.encodeStart(JsonOps.INSTANCE, this.fantasyDefinition())
                .resultOrPartial(msg -> SlashBlade.LOGGER.error("Failed to encode fantasy: {}", msg))
                .ifPresent(json -> buffer.writeUtf(json.toString()));
    }

    public static FDRequestDefinition fromNetwork(FriendlyByteBuf buffer) {
        ResourceLocation name = buffer.readResourceLocation();
        int proud = buffer.readInt();
        int kill = buffer.readInt();
        int refine = buffer.readInt();
        var enchantments = buffer
                .readList((buf) -> new EnchantmentDefinition(buf.readResourceLocation(), buf.readByte()));
        var types = buffer.readList((buf) -> SwordType.valueOf(buf.readUtf().toUpperCase()));

        String fantasyJsonStr = buffer.readUtf();
        FantasyDefinition fantasy = FantasyDefinition.CODEC
                .parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(fantasyJsonStr).getAsJsonObject())
                .resultOrPartial(msg -> SlashBlade.LOGGER.error("Failed to parse fantasy from network: {}", msg))
                .orElseGet(() -> FantasyDefinition.Builder.newInstance().build());

        return new FDRequestDefinition(name, proud, kill, refine, enchantments, types, fantasy);
    }

    public void initItemStack(ItemStack blade) {
        var state = blade.getCapability(ItemSlashBlade.BLADESTATE).orElse(new SlashBladeState(blade));
        state.setNonEmpty();
        if (!this.name.equals(SlashBlade.prefix("none"))) {
            state.setTranslationKey(getTranslationKey());
        }
        state.setProudSoulCount(proudSoulCount());
        state.setKillCount(killCount());
        state.setRefine(refineCount());

        this.enchantments()
                .forEach(enchantment -> {
                    var ench = ForgeRegistries.ENCHANTMENTS.getValue(enchantment.getEnchantmentID());
                    if (ench != null) {
                        blade.enchant(ench, enchantment.getEnchantmentLevel());
                    }
                });
        this.defaultType.forEach(type -> {
            switch (type) {
                case BEWITCHED -> state.setDefaultBewitched(true);
                case BROKEN -> {
                    blade.setDamageValue(blade.getMaxDamage() - 1);
                    state.setBroken(true);
                }
                case SEALED -> state.setSealed(true);
                default -> {
                }
            }
        });

        blade.getOrCreateTag().put("bladeState", state.serializeNBT());

        var fdState = blade.getCapability(ItemFantasySlashBlade.FDBLADESTATE).orElse(new FantasySlashBladeState(blade));
        fdState.setSpecialCharge(fantasyDefinition().getSpecialCharge());
        fdState.setMaxSpecialCharge(fantasyDefinition().getMaxSpecialCharge());
        fdState.setSpecialLore(fantasyDefinition().getSpecialLore());
        fdState.setSpecialEffectLore(fantasyDefinition().getSpecialEffectLore());
        fdState.setSpecialAttackLore(fantasyDefinition().getSpecialAttackLore());
        fdState.setSpecialType(fantasyDefinition().getSpecialType());
        fdState.setSpecialChargeName(fantasyDefinition().getSpecialChargeName());
        fdState.setSpecialAttackEffect(fantasyDefinition().getSpecialAttackEffect());
        blade.getOrCreateTag().put("fdBladeState", fdState.serializeNBT());
    }

    public boolean test(ItemStack blade) {
        if (blade == null || blade.isEmpty()) {
            return false;
        }
        if (!blade.getCapability(ItemSlashBlade.BLADESTATE).isPresent()) {
            return false;
        }
        var state = blade.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(NullPointerException::new);
        boolean nameCheck;
        if (this.name.equals(SlashBlade.prefix("none"))) {
            nameCheck = state.getTranslationKey().isBlank();
        } else {
            nameCheck = state.getTranslationKey().equals(getTranslationKey());
        }
        boolean proudCheck = state.getProudSoulCount() >= this.proudSoulCount();
        boolean killCheck = state.getKillCount() >= this.killCount();
        boolean refineCheck = state.getRefine() >= this.refineCount();

        for (var enchantment : this.enchantments()) {
            var ench = ForgeRegistries.ENCHANTMENTS.getValue(enchantment.getEnchantmentID());
            if (ench != null && blade.getEnchantmentLevel(ench) < enchantment.getEnchantmentLevel()) {
                return false;
            }
        }

        boolean types = SwordType.from(blade).containsAll(this.defaultType());

        if (!(nameCheck && proudCheck && killCheck && refineCheck && types)) {
            return false;
        }

        if (!blade.getCapability(ItemFantasySlashBlade.FDBLADESTATE).isPresent()) {
            return false;
        }
        var fdState = blade.getCapability(ItemFantasySlashBlade.FDBLADESTATE).orElseThrow(NullPointerException::new);

        if (fdState.getSpecialCharge() < this.fantasyDefinition().getSpecialCharge())
            return false;
        if (fdState.getMaxSpecialCharge() < this.fantasyDefinition().getMaxSpecialCharge())
            return false;
        if (fdState.getSpecialLore() < this.fantasyDefinition().getSpecialLore())
            return false;
        if (fdState.getSpecialEffectLore() < this.fantasyDefinition().getSpecialEffectLore())
            return false;
        if (fdState.getSpecialAttackLore() < this.fantasyDefinition().getSpecialAttackLore())
            return false;

        if (!"Null".equals(this.fantasyDefinition().getSpecialType()) &&
                !this.fantasyDefinition().getSpecialType().equals(fdState.getSpecialType()))
            return false;

        if (!"Null".equals(this.fantasyDefinition().getSpecialChargeName()) &&
                !this.fantasyDefinition().getSpecialChargeName().equals(fdState.getSpecialChargeName()))
            return false;

        if (!"Null".equals(this.fantasyDefinition().getSpecialAttackEffect()) &&
                !this.fantasyDefinition().getSpecialAttackEffect().equals(fdState.getSpecialAttackEffect()))
            return false;

        return true;
    }

    public String getTranslationKey() {
        return Util.makeDescriptionId("item", this.name());
    }

    public static class Builder {
        private ResourceLocation name;
        private int proudCount;
        private int killCount;
        private int refineCount;
        private final List<EnchantmentDefinition> enchantments;
        private final List<SwordType> defaultType;
        private FantasyDefinition fantasyDefinition;

        private Builder() {
            this.name = SlashBlade.prefix("none");
            this.proudCount = 0;
            this.killCount = 0;
            this.refineCount = 0;
            this.enchantments = new ArrayList<>();
            this.defaultType = new ArrayList<>();
            this.fantasyDefinition = FantasyDefinition.Builder.newInstance().build();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(ResourceLocation name) {
            this.name = name;
            return this;
        }

        public Builder proudSoul(int proudCount) {
            this.proudCount = proudCount;
            return this;
        }

        public Builder killCount(int killCount) {
            this.killCount = killCount;
            return this;
        }

        public Builder refineCount(int refineCount) {
            this.refineCount = refineCount;
            return this;
        }

        public Builder addEnchantment(EnchantmentDefinition... enchantments) {
            Collections.addAll(this.enchantments, enchantments);
            return this;
        }

        public Builder addSwordType(SwordType... types) {
            Collections.addAll(this.defaultType, types);
            return this;
        }

        public Builder fantasyDefinition(FantasyDefinition fantasyDefinition) {
            this.fantasyDefinition = fantasyDefinition;
            return this;
        }

        public Builder specialCharge(int specialCharge) {
            this.fantasyDefinition = FantasyDefinition.Builder.newInstance()
                    .specialCharge(specialCharge)
                    .maxSpecialCharge(this.fantasyDefinition.getMaxSpecialCharge())
                    .specialLore(this.fantasyDefinition.getSpecialLore())
                    .specialEffectLore(this.fantasyDefinition.getSpecialEffectLore())
                    .specialAttackLore(this.fantasyDefinition.getSpecialAttackLore())
                    .specialType(this.fantasyDefinition.getSpecialType())
                    .specialChargeName(this.fantasyDefinition.getSpecialChargeName())
                    .specialAttackEffect(this.fantasyDefinition.getSpecialAttackEffect())
                    .build();
            return this;
        }

        public Builder maxSpecialCharge(int maxSpecialCharge) {
            this.fantasyDefinition = FantasyDefinition.Builder.newInstance()
                    .specialCharge(this.fantasyDefinition.getSpecialCharge())
                    .maxSpecialCharge(maxSpecialCharge)
                    .specialLore(this.fantasyDefinition.getSpecialLore())
                    .specialEffectLore(this.fantasyDefinition.getSpecialEffectLore())
                    .specialAttackLore(this.fantasyDefinition.getSpecialAttackLore())
                    .specialType(this.fantasyDefinition.getSpecialType())
                    .specialChargeName(this.fantasyDefinition.getSpecialChargeName())
                    .specialAttackEffect(this.fantasyDefinition.getSpecialAttackEffect())
                    .build();
            return this;
        }

        public Builder specialLore(int specialLore) {
            this.fantasyDefinition = FantasyDefinition.Builder.newInstance()
                    .specialCharge(this.fantasyDefinition.getSpecialCharge())
                    .maxSpecialCharge(this.fantasyDefinition.getMaxSpecialCharge())
                    .specialLore(specialLore)
                    .specialEffectLore(this.fantasyDefinition.getSpecialEffectLore())
                    .specialAttackLore(this.fantasyDefinition.getSpecialAttackLore())
                    .specialType(this.fantasyDefinition.getSpecialType())
                    .specialChargeName(this.fantasyDefinition.getSpecialChargeName())
                    .specialAttackEffect(this.fantasyDefinition.getSpecialAttackEffect())
                    .build();
            return this;
        }

        public Builder specialEffectLore(int specialEffectLore) {
            this.fantasyDefinition = FantasyDefinition.Builder.newInstance()
                    .specialCharge(this.fantasyDefinition.getSpecialCharge())
                    .maxSpecialCharge(this.fantasyDefinition.getMaxSpecialCharge())
                    .specialLore(this.fantasyDefinition.getSpecialLore())
                    .specialEffectLore(specialEffectLore)
                    .specialAttackLore(this.fantasyDefinition.getSpecialAttackLore())
                    .specialType(this.fantasyDefinition.getSpecialType())
                    .specialChargeName(this.fantasyDefinition.getSpecialChargeName())
                    .specialAttackEffect(this.fantasyDefinition.getSpecialAttackEffect())
                    .build();
            return this;
        }

        public Builder specialAttackLore(int specialAttackLore) {
            this.fantasyDefinition = FantasyDefinition.Builder.newInstance()
                    .specialCharge(this.fantasyDefinition.getSpecialCharge())
                    .maxSpecialCharge(this.fantasyDefinition.getMaxSpecialCharge())
                    .specialLore(this.fantasyDefinition.getSpecialLore())
                    .specialEffectLore(this.fantasyDefinition.getSpecialEffectLore())
                    .specialAttackLore(specialAttackLore)
                    .specialType(this.fantasyDefinition.getSpecialType())
                    .specialChargeName(this.fantasyDefinition.getSpecialChargeName())
                    .specialAttackEffect(this.fantasyDefinition.getSpecialAttackEffect())
                    .build();
            return this;
        }

        public Builder specialType(String specialType) {
            this.fantasyDefinition = FantasyDefinition.Builder.newInstance()
                    .specialCharge(this.fantasyDefinition.getSpecialCharge())
                    .maxSpecialCharge(this.fantasyDefinition.getMaxSpecialCharge())
                    .specialLore(this.fantasyDefinition.getSpecialLore())
                    .specialEffectLore(this.fantasyDefinition.getSpecialEffectLore())
                    .specialAttackLore(this.fantasyDefinition.getSpecialAttackLore())
                    .specialType(specialType)
                    .specialChargeName(this.fantasyDefinition.getSpecialChargeName())
                    .specialAttackEffect(this.fantasyDefinition.getSpecialAttackEffect())
                    .build();
            return this;
        }

        public Builder specialChargeName(String specialChargeName) {
            this.fantasyDefinition = FantasyDefinition.Builder.newInstance()
                    .specialCharge(this.fantasyDefinition.getSpecialCharge())
                    .maxSpecialCharge(this.fantasyDefinition.getMaxSpecialCharge())
                    .specialLore(this.fantasyDefinition.getSpecialLore())
                    .specialEffectLore(this.fantasyDefinition.getSpecialEffectLore())
                    .specialAttackLore(this.fantasyDefinition.getSpecialAttackLore())
                    .specialType(this.fantasyDefinition.getSpecialType())
                    .specialChargeName(specialChargeName)
                    .specialAttackEffect(this.fantasyDefinition.getSpecialAttackEffect())
                    .build();
            return this;
        }

        public Builder specialAttackEffect(String specialAttackEffect) {
            this.fantasyDefinition = FantasyDefinition.Builder.newInstance()
                    .specialCharge(this.fantasyDefinition.getSpecialCharge())
                    .maxSpecialCharge(this.fantasyDefinition.getMaxSpecialCharge())
                    .specialLore(this.fantasyDefinition.getSpecialLore())
                    .specialEffectLore(this.fantasyDefinition.getSpecialEffectLore())
                    .specialAttackLore(this.fantasyDefinition.getSpecialAttackLore())
                    .specialType(this.fantasyDefinition.getSpecialType())
                    .specialChargeName(this.fantasyDefinition.getSpecialChargeName())
                    .specialAttackEffect(specialAttackEffect)
                    .build();
            return this;
        }

        public FDRequestDefinition build() {
            return new FDRequestDefinition(name, proudCount, killCount, refineCount, enchantments, defaultType,
                    fantasyDefinition);
        }
    }
}

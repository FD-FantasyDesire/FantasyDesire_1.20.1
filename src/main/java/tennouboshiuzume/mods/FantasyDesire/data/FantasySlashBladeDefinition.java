package tennouboshiuzume.mods.FantasyDesire.data;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDItems;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.FantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class FantasySlashBladeDefinition {
    public static final Codec<FantasySlashBladeDefinition> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(ResourceLocation.CODEC.fieldOf("name").forGetter(FantasySlashBladeDefinition::getName),
                RenderDefinition.CODEC.fieldOf("render").forGetter(FantasySlashBladeDefinition::getRenderDefinition),
                PropertiesDefinition.CODEC.fieldOf("properties").forGetter(FantasySlashBladeDefinition::getStateDefinition),
                FantasyDefinition.CODEC.fieldOf("fantasy").forGetter(FantasySlashBladeDefinition::getFantasyDefinition),
                EnchantmentDefinition.CODEC.listOf().optionalFieldOf("enchantments", Lists.newArrayList()).forGetter(FantasySlashBladeDefinition::getEnchantments)
        ).apply(instance, FantasySlashBladeDefinition::new);
    });
    public static final ResourceKey<Registry<FantasySlashBladeDefinition>> REGISTRY_KEY = ResourceKey.createRegistryKey(FantasyDesire.prefix("fantasyslashblade"));
    private final ResourceLocation name;
    private final RenderDefinition renderDefinition;
    private final PropertiesDefinition stateDefinition;
    private final FantasyDefinition fantasyDefinition;
    private final List<EnchantmentDefinition> enchantments;
    public static final FantasySlashBladeDefinition.BladeComparator COMPARATOR = new FantasySlashBladeDefinition.BladeComparator();

    public FantasySlashBladeDefinition(ResourceLocation name, RenderDefinition renderDefinition, PropertiesDefinition stateDefinition, FantasyDefinition fantasyDefinition, List<EnchantmentDefinition> enchantments) {
        this.name = name;
        this.renderDefinition = renderDefinition;
        this.stateDefinition = stateDefinition;
        this.fantasyDefinition = fantasyDefinition;
        this.enchantments = enchantments;
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public String getTranslationKey() {
        return Util.makeDescriptionId("item", this.getName());
    }

    public RenderDefinition getRenderDefinition() {
        return this.renderDefinition;
    }

    public PropertiesDefinition getStateDefinition() {
        return this.stateDefinition;
    }

    public FantasyDefinition getFantasyDefinition() {
        return this.fantasyDefinition;
    }

    public List<EnchantmentDefinition> getEnchantments() {
        return this.enchantments;
    }

    public ItemStack getBlade() {
        return this.getBlade(FDItems.fantasyslashblade);
    }

    public ItemStack getBlade(Item bladeItem) {
        ItemStack result = new ItemStack(bladeItem);
        ISlashBladeState state = (ISlashBladeState)result.getCapability(ItemSlashBlade.BLADESTATE).orElse(new SlashBladeState(result));
        state.setBaseAttackModifier(this.stateDefinition.getBaseAttackModifier());
        state.setMaxDamage(this.stateDefinition.getMaxDamage());
        state.setComboRoot(this.stateDefinition.getComboRoot());
        state.setSlashArtsKey(this.stateDefinition.getSpecialAttackType());
        List<ResourceLocation> var10000 = this.stateDefinition.getSpecialEffects();
        Objects.requireNonNull(state);
        var10000.forEach(state::addSpecialEffect);
        this.stateDefinition.getDefaultType().forEach((type) -> {
            switch (type) {
                case BEWITCHED:
                    state.setDefaultBewitched(true);
                    break;
                case BROKEN:
                    result.setDamageValue(result.getMaxDamage() - 1);
                    state.setBroken(true);
                    break;
                case SEALED:
                    state.setSealed(true);
            }

        });
        state.setModel(this.renderDefinition.getModelName());
        state.setTexture(this.renderDefinition.getTextureName());
        state.setColorCode(this.renderDefinition.getSummonedSwordColor());
        state.setEffectColorInverse(this.renderDefinition.isSummonedSwordColorInverse());
        state.setCarryType(this.renderDefinition.getStandbyRenderType());
        if (!this.getName().equals(SlashBlade.prefix("none"))) {
            state.setTranslationKey(this.getTranslationKey());
        }
        result.getOrCreateTag().put("bladeState", state.serializeNBT());

        IFantasySlashBladeState fdState = (IFantasySlashBladeState)result.getCapability(ItemFantasySlashBlade.FDBLADESTATE).orElse(new FantasySlashBladeState(result));
        fdState.setSpecialCharge(this.fantasyDefinition.getSpecialCharge());
        fdState.setMaxSpecialCharge(this.fantasyDefinition.getMaxSpecialCharge());
        fdState.setSpecialLore(this.fantasyDefinition.getSpecialLore());
        fdState.setSpecialEffectLore(this.fantasyDefinition.getSpecialEffectLore());
        fdState.setSpecialAttackLore(this.fantasyDefinition.getSpecialAttackLore());
        fdState.setSpecialType(this.fantasyDefinition.getSpecialType());
        result.getOrCreateTag().put("fdBladeState",fdState.serializeNBT());

        Iterator var4 = this.enchantments.iterator();

        while(var4.hasNext()) {
            EnchantmentDefinition instance = (EnchantmentDefinition)var4.next();
            Enchantment enchantment = (Enchantment) ForgeRegistries.ENCHANTMENTS.getValue(instance.getEnchantmentID());
            result.enchant(enchantment, instance.getEnchantmentLevel());
        }

        return result;
    }

    private static class BladeComparator implements Comparator<Holder.Reference<FantasySlashBladeDefinition>> {
        private BladeComparator() {
        }

        public int compare(Holder.Reference<FantasySlashBladeDefinition> left, Holder.Reference<FantasySlashBladeDefinition> right) {
            ResourceLocation leftKey = left.key().location();
            ResourceLocation rightKey = right.key().location();
            boolean checkSame = leftKey.getNamespace().equalsIgnoreCase(rightKey.getNamespace());
            if (!checkSame) {
                if (leftKey.getNamespace().equalsIgnoreCase("slashblade")) {
                    return -1;
                }

                if (rightKey.getNamespace().equalsIgnoreCase("slashblade")) {
                    return 1;
                }
            }

            String leftName = leftKey.toString();
            String rightName = rightKey.toString();
            return leftName.compareToIgnoreCase(rightName);
        }
    }
}

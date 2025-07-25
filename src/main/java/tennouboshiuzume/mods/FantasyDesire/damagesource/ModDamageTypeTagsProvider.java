package tennouboshiuzume.mods.FantasyDesire.damagesource;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.common.data.ExistingFileHelper;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModDamageTypeTagsProvider extends DamageTypeTagsProvider {

    public ModDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, provider, FantasyDesire.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
//      无视护甲
        this.tag(DamageTypeTags.BYPASSES_ARMOR)
                .add(FDDamageTypes.DIMENSION)
                .add(FDDamageTypes.LUST)
                .add(FDDamageTypes.OMEGA)
                .add(FDDamageTypes.ETERNITY)
        ;
//      无视无敌帧
        this.tag(DamageTypeTags.BYPASSES_INVULNERABILITY)
                .add(FDDamageTypes.DIMENSION)
                .add(FDDamageTypes.OMEGA)
                .add(FDDamageTypes.ETERNITY)
                .add(FDDamageTypes.ECHO)
                .add(FDDamageTypes.WRATH)
                .add(FDDamageTypes.LUST)
                .add(FDDamageTypes.SLOTH)
                .add(FDDamageTypes.GLUTTONY)
                .add(FDDamageTypes.GLOOM)
                .add(FDDamageTypes.PRIDE)
                .add(FDDamageTypes.ENVY)
        ;
//      无视盾牌
        this.tag(DamageTypeTags.BYPASSES_SHIELD)
                .add(FDDamageTypes.DIMENSION)
                .add(FDDamageTypes.ECHO)
                .add(FDDamageTypes.WRATH);

//      火焰伤害
        this.tag(DamageTypeTags.IS_FIRE)
                .add(FDDamageTypes.WRATH);
//      射弹伤害
        this.tag(DamageTypeTags.IS_PROJECTILE)
                .add(FDDamageTypes.LUST);
//      冰冻伤害
        this.tag(DamageTypeTags.IS_FREEZING).
                add(FDDamageTypes.SLOTH);
//      溺水
        this.tag(DamageTypeTags.IS_DROWNING)
                .add(FDDamageTypes.GLOOM);
//      无视伤害抗性
        this.tag(DamageTypeTags.BYPASSES_RESISTANCE)
                .add(FDDamageTypes.GLUTTONY);
    }
// 也许我需要一个测试武器来校验每种伤害对敌人的效果，但不是现在

}

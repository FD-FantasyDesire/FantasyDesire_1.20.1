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
                .add(FDDamageSource.DIMENSION)
                .add(FDDamageSource.LUST)
                .add(FDDamageSource.OMEGA)
                .add(FDDamageSource.ETERNITY)
        ;
//      无视无敌帧
        this.tag(DamageTypeTags.BYPASSES_INVULNERABILITY)
                .add(FDDamageSource.DIMENSION)
                .add(FDDamageSource.OMEGA)
                .add(FDDamageSource.ETERNITY)
                .add(FDDamageSource.RESOLUTION)
                .add(FDDamageSource.ECHO)
                .add(FDDamageSource.WRATH)
                .add(FDDamageSource.LUST)
                .add(FDDamageSource.SLOTH)
                .add(FDDamageSource.GLUTTONY)
                .add(FDDamageSource.GLOOM)
                .add(FDDamageSource.PRIDE)
                .add(FDDamageSource.ENVY)
        ;
//      无视盾牌
        this.tag(DamageTypeTags.BYPASSES_SHIELD)
                .add(FDDamageSource.OMEGA)
                .add(FDDamageSource.DIMENSION)
                .add(FDDamageSource.WRATH);

//      火焰伤害
        this.tag(DamageTypeTags.IS_FIRE)
                .add(FDDamageSource.WRATH);

//      射弹伤害
        this.tag(DamageTypeTags.IS_PROJECTILE)
                .add(FDDamageSource.LUST);

//      冰冻伤害
        this.tag(DamageTypeTags.IS_FREEZING)
                .add(FDDamageSource.SLOTH);

//      溺水伤害
        this.tag(DamageTypeTags.IS_DROWNING)
                .add(FDDamageSource.GLOOM);

//      无视抗性药水效果
        this.tag(DamageTypeTags.BYPASSES_RESISTANCE)
                .add(FDDamageSource.OMEGA)
                .add(FDDamageSource.GLUTTONY)
                .add(FDDamageSource.DIMENSION);
    }
}

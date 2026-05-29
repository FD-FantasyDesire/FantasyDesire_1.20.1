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
                .add(FDDamageSource.OMEGA)
                .add(FDDamageSource.ETERNITY)
        ;
//      无视无敌
        this.tag(DamageTypeTags.BYPASSES_INVULNERABILITY)
                .add(FDDamageSource.ETERNITY)
        ;
//      无视盾牌
        this.tag(DamageTypeTags.BYPASSES_SHIELD)
                .add(FDDamageSource.OMEGA)
                .add(FDDamageSource.WRATH);
//      火焰伤害
        this.tag(DamageTypeTags.IS_FIRE);
//      射弹伤害
        this.tag(DamageTypeTags.IS_PROJECTILE);

//      冰冻伤害
        this.tag(DamageTypeTags.IS_FREEZING);
//      溺水伤害
        this.tag(DamageTypeTags.IS_DROWNING);

//      无视抗性药水效果
        this.tag(DamageTypeTags.BYPASSES_RESISTANCE)
                .add(FDDamageSource.OMEGA)
                .add(FDDamageSource.DIMENSION);
    }
}

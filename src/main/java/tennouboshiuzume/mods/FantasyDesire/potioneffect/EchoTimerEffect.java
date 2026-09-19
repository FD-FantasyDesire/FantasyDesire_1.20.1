package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import tennouboshiuzume.mods.FantasyDesire.utils.EchoDamageHelper;

public class EchoTimerEffect extends MobEffect {
    public EchoTimerEffect() {
        super(MobEffectCategory.HARMFUL, 0x000000); // 黑色或任意颜色，反正会被隐藏
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return false;
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        // do nothing
    }

    @Override
    public void removeAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);

        // 如果实体已死亡，不在这里处理，由 EchoDeathEventHandler 处理
        if (pLivingEntity.level().isClientSide || !pLivingEntity.isAlive()) {
            return;
        }

        // 如果实体仍然有该效果，说明是因为刷新效果调用的 removeAttributeModifiers，此时不应该引爆
        if (pLivingEntity.hasEffect(this)) {
            return;
        }

        EchoDamageHelper.detonateSingle(pLivingEntity);
    }
}

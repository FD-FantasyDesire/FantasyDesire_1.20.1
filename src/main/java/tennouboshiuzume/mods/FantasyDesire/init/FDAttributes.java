package tennouboshiuzume.mods.FantasyDesire.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.potioneffect.VoidStrikeEffect;

/**
 * 自定义属性注册表。
 *
 * 寒霜风暴半径与虚空强袭层数由各自效果维护临时加值修改器，
 * 随属性同步包下发客户端，并在效果移除时清理。
 * 风暴强度（FROST_STORM_STRENGTH）以 1 为基值，风暴成长通过加值修改器写入。
 */
public class FDAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE,
            FantasyDesire.MODID);

    /** 寒霜风暴领域半径（0~64） */
    public static final RegistryObject<Attribute> FROST_STORM_RADIUS = ATTRIBUTES.register("frost_storm_radius",
            () -> new RangedAttribute("attribute.fantasydesire.frost_storm_radius", 0.0D, 0.0D, 64.0D).setSyncable(true));
    /** 寒霜风暴领域强度（基值 1，上限 10） */
    public static final RegistryObject<Attribute> FROST_STORM_STRENGTH = ATTRIBUTES.register("frost_storm_strength",
            () -> new RangedAttribute("attribute.fantasydesire.frost_storm_strength", 1.0D, 0.0D, 10.0D).setSyncable(true));
    /** 虚空强袭层数，等于效果 AMP + 1（0~50） */
    public static final RegistryObject<Attribute> VOID_STRIKE_STACK = ATTRIBUTES.register("void_strike_stack",
            () -> new RangedAttribute("attribute.fantasydesire.void_strike_stack", 0.0D, 0.0D,
                    VoidStrikeEffect.MAX_STACKS).setSyncable(true));
    /** 目标身上所有攻击者的 Echo 伤害计数合计值 */
    public static final RegistryObject<Attribute> TOTAL_ECHO_DAMAGE = ATTRIBUTES.register("total_echo_damage",
            () -> new RangedAttribute("attribute.fantasydesire.total_echo_damage", 0.0D, 0.0D, 1.0E9D).setSyncable(true));

    /** 望的有效点数上限，同时约束每把刀的光环数量。 */
    public static final int MAX_LC_POINTS = 1024;
    public static final RegistryObject<Attribute> FD_LC_MANG = ATTRIBUTES.register("fd_lc_mang",
            () -> new RangedAttribute("attribute.fantasydesire.fd_lc_mang", 0.0D, 0.0D, MAX_LC_POINTS)
                    .setSyncable(true));
    /** 心：第 1 档金色流焰不透明度系数为 20%，第 18 档为 100%；0 不渲染。 */
    public static final RegistryObject<Attribute> FD_LC_SHIN = ATTRIBUTES.register("fd_lc_shin",
            () -> new RangedAttribute("attribute.fantasydesire.fd_lc_shin", 0.0D, 0.0D, MAX_LC_POINTS)
                    .setSyncable(true));

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(FDAttributes::onEntityAttributeModification);
        System.out.println("[FantasyDesire] Registered custom attributes on MOD event bus.");
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> entityType : event.getTypes()) {
            event.add(entityType, FROST_STORM_RADIUS.get());
            event.add(entityType, FROST_STORM_STRENGTH.get());
            event.add(entityType, VOID_STRIKE_STACK.get());
            event.add(entityType, TOTAL_ECHO_DAMAGE.get());
            event.add(entityType, FD_LC_SHIN.get());
        }
        event.add(EntityType.PLAYER, FD_LC_MANG.get());
    }

    /** 读取实体当前寒霜风暴半径（无修改器时返回 0） */
    public static float getStormRadius(LivingEntity entity) {
        var attr = entity.getAttribute(FROST_STORM_RADIUS.get());
        return attr == null ? 0.0F : (float) attr.getValue();
    }

    /** 读取实体当前寒霜风暴强度（无修改器时返回属性基值 1） */
    public static float getStormStrength(LivingEntity entity) {
        var attr = entity.getAttribute(FROST_STORM_STRENGTH.get());
        return attr == null ? 0.0F : (float) attr.getValue();
    }

    public static float getVoidStrikeStack(LivingEntity entity) {
        var attr = entity.getAttribute(VOID_STRIKE_STACK.get());
        return attr == null ? 0.0F : (float) attr.getValue();
    }

    public static float getTotalEchoDamage(LivingEntity entity) {
        var attr = entity.getAttribute(TOTAL_ECHO_DAMAGE.get());
        return attr == null ? 0.0F : (float) attr.getValue();
    }

    /** 无属性、负数或非有限值不产生气场；不改变服务端的属性数值。 */
    public static float getShinStrength(LivingEntity entity) {
        var attr = entity.getAttribute(FD_LC_SHIN.get());
        double value = attr == null ? 0.0D : attr.getValue();
        return Double.isFinite(value) ? (float) Math.max(0.0D, Math.min(MAX_LC_POINTS, value)) : 0.0F;
    }

    public static void syncTotalEchoDamage(LivingEntity entity, double total, java.util.UUID modifierId) {
        var attr = entity.getAttribute(TOTAL_ECHO_DAMAGE.get());
        if (attr == null) {
            return;
        }
        if (total <= 0.0D) {
            attr.removeModifier(modifierId);
            return;
        }
        var old = attr.getModifier(modifierId);
        if (old == null || old.getAmount() != total) {
            attr.removeModifier(modifierId);
            attr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    modifierId, "fd_total_echo_damage", total,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        }
    }

    public static void clearTotalEchoDamage(LivingEntity entity, java.util.UUID modifierId) {
        var attr = entity.getAttribute(TOTAL_ECHO_DAMAGE.get());
        if (attr != null) {
            attr.removeModifier(modifierId);
        }
    }
}

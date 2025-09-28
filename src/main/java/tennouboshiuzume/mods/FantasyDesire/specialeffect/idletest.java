package tennouboshiuzume.mods.FantasyDesire.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;

@Mod.EventBusSubscriber
public class idletest {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager.get(new CapabilityToken<IFantasySlashBladeState>() {
    });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager.get(new CapabilityToken<ISlashBladeState>() {
    });

//    @SubscribeEvent
//    public static void fdupdateevent(SlashBladeEvent.UpdateEvent event) {
//        ItemStack blade = event.getBlade();
//        Entity player = event.getEntity();
//        if (blade.getItem() instanceof ItemFantasySlashBlade && player instanceof Player && ((Player) player).getMainHandItem()==blade) {
//            ISlashBladeState state = blade.getCapability(BLADESTATE).orElseThrow(NullPointerException::new);
//            IFantasySlashBladeState fdState = blade.getCapability(FDBLADESTATE).orElseThrow(NullPointerException::new);
//            if (!state.getTranslationKey().equals("item.fantasydesire.pure_snow")) return;
//            int color = ColorUtils.getSmoothTransitionColor(player.level().getDayTime() % 126, 126, true);
//            state.setColorCode(color);
//            int timeStep = (int) ((player.level().getDayTime() + 8) % 126);
//            int damageTypeIndex = (timeStep / 18);
//            fdState.setSpecialAttackEffect(damageTypes[damageTypeIndex]);
//        }
//    }

//    EntityAbstractSummonedSword
//
//    private static final List<Entity> toRemoveNextTick = new ArrayList<>();
//
//
//    @SubscribeEvent
//    public static void onGlobalItem(EntityJoinLevelEvent event){
//        Entity entity = event.getEntity();
//        if (!(entity instanceof BladeItemEntity)) return;
//
//        ItemStack blade = ((BladeItemEntity) entity).getItem();
//
//        blade.getCapability(BLADESTATE).ifPresent((s) -> {
//            if (!s.getTranslationKey().equals("item.fantasydesire.chikeflare")) return;
//
//            CompoundTag tag = blade.getOrCreateTag();
//
//            if (!tag.contains("OwnerName") || !tag.contains("ItemUUID")) {
//                Player owner = getNearestPlayer(entity, 3.0);
//                if (owner == null) return;
//
//                tag.putString("OwnerName", owner.getGameProfile().getName());
//                tag.putString("ItemUUID", UUID.randomUUID().toString());
//            }
//
//            String ownerName = tag.getString("OwnerName");
//            GlobalItemManager.saveItemForPlayer(ownerName, blade);
//
//            // 设置实体隐藏、防掉落等
//            entity.setInvisible(true);
//            entity.setNoGravity(true);
//            entity.setDeltaMovement(Vec3.ZERO);
//            entity.setSilent(true);
//
//            // 添加进待删除列表
//            toRemoveNextTick.add(entity);
////            event.isCanceled();
//        });
//    }
//    @SubscribeEvent
//    public static void onServerTick(TickEvent.ServerTickEvent event) {
//        if (event.phase != TickEvent.Phase.END) return;
//
//        for (Entity e : toRemoveNextTick) {
//            if (!e.isRemoved()) {
//                e.discard(); // 删除实体
//            }
//        }
//        toRemoveNextTick.clear();
//    }
//
//
//
//    private static final String COMMAND = "recallblade";
//
//
//    @SubscribeEvent
//    public static void CallGlobalItem(ServerChatEvent event){
//        ServerPlayer player = event.getPlayer();
//        String message = event.getMessage().getString();
//        System.out.println(message);
//        // 判断消息是否为召回命令
//        if (message.equalsIgnoreCase(COMMAND)) {
//            // 从 GlobalItemManager 加载物品
//            ItemStack bladeItem = GlobalItemManager.loadAndClearItemForPlayer(player.getGameProfile().getName());
//            if (!bladeItem.isEmpty()) {
//                // 如果成功加载物品，添加到玩家背包
//                if (!player.getInventory().add(bladeItem)) {
//                    // 如果背包满了，可以掉落在玩家附近
//                    return;
//                }
//                // 发送确认消息给玩家
//                System.out.println("susscess");
//            } else {
//                // 如果没有物品
//                System.out.println("fail");
//            }
//        }
//
//    }

//    public static Player getNearestPlayer(Entity entity, double radius) {
//        Level level = entity.level();
//        AABB area = entity.getBoundingBox().inflate(radius);
//
//        return level.getEntitiesOfClass(Player.class, area).stream()
//                .min(Comparator.comparingDouble(p -> p.distanceToSqr(entity)))
//                .orElse(null); // 没有玩家就返回 null
//    }
    //      根据伤害类型作出不同的修正
//        暴怒 点燃敌人
//        色欲 使攻击者回复0.2生命值
//        暴食 使攻击者回复0.2饥饿值
//        忧郁 消耗敌人的氧气条
//        傲慢 对拥有护甲的敌人1.5x伤害
//        嫉妒 对生命值大于你的敌人造成3x伤害
//        次元 伤害的50%会转化为魔法伤害
//        永劫 伤害的10%转化为生命值上限削减
//        吸收 使攻击者吸收同等生命值，溢出部分的10%转化为额外生命，最大100
//        决断 伤害的20%转化为真实伤害

//    @SubscribeEvent
//    public static void OnHurt(LivingHurtEvent event) {
//        DamageSource source = event.getSource();
//        Holder<DamageType> holder = source.typeHolder();
//        holder.unwrapKey().ifPresent(key -> System.out.println("伤害类型ID: " + key.location()));
//        if (holder.unwrapKey().map(key ->
//                key.location().getNamespace().equals(FantasyDesire.MODID)
//        ).orElse(false)) {
//            return;
//        }
//        Entity attacker = source.getEntity(); // 攻击者
//        LivingEntity target = event.getEntity(); // 受击者
//        float original = event.getAmount();
//        if (attacker instanceof Player) {
//            ItemStack blade = ((Player) attacker).getMainHandItem();
//            if (blade.getItem() instanceof ItemFantasySlashBlade) {
//                blade.getCapability(FDBLADESTATE).ifPresent((s) -> {
//                    blade.getCapability(BLADESTATE).ifPresent((b) -> {
//                        if (s.getSpecialAttackEffect().equals("dimension") && b.getTranslationKey().equals("item.fantasydesire.chikeflare")) {
//                            event.setAmount(original * 0.5f);
//                            target.invulnerableTime = 0;
//                            target.hurtTime = 0;
//                            target.hurt(DamageUtils.createWithEntity(FDDamageTypes.DIMENSION, (LivingEntity) attacker, attacker.level().registryAccess()), original * 0.5f);
//                        }
//                    });
//                });
//            }
//        }
//    }

//    @SubscribeEvent
//    public static void OnDamage(LivingDamageEvent event) {
//        DamageSource source = event.getSource();
//        Entity attacker = source.getEntity();
//        LivingEntity target = event.getEntity();
//        Holder<DamageType> holder = source.typeHolder();
//        float original = event.getAmount();
////        if (attacker instanceof Player) {
////            ItemStack blade = ((Player) attacker).getMainHandItem();
////            if (blade.getItem() instanceof ItemFantasySlashBlade) {
////                blade.getCapability(FDBLADESTATE).ifPresent((s) -> {
////                    blade.getCapability(BLADESTATE).ifPresent((b) -> {
////
////                        if (s.getSpecialAttackEffect().equals("dimension") && b.getTranslationKey().equals("item.fantasydesire.chikeflare")){
////                            event.setAmount(original*0.5f);
////                        }
////
////                    });
////                });
////            }
////        }
////
////        if (holder.unwrapKey().filter(key -> key.equals(FDDamageTypes.ETERNITY)).isPresent() && target.getArmorValue()>0) {
////            event.setAmount(original * 1.5f);
////        }
////
////        if (holder.unwrapKey().filter(key -> key.equals(FDDamageTypes.PRIDE)).isPresent() && target.getArmorValue()>0) {
////            event.setAmount(original * 1.5f);
////        }

//    }


    public static String[] damageTypes = {"wrath", "lust", "sloth", "gluttony", "gloom", "pride", "envy"};

}

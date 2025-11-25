package tennouboshiuzume.mods.FantasyDesire.specialeffect.globalevent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber
public class DelayTaskManager {

    private static final List<Triple<LivingEntity, Integer, Runnable>> TASKS = new ArrayList<>();

    /**
     * 添加延迟任务
     * @param entity 目标实体
     * @param delayTick 延迟 tick 数
     * @param action 延迟执行动作
     */
    public static void add(LivingEntity entity, int delayTick, Runnable action) {
        TASKS.add(new Triple<>(entity, delayTick, action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Triple<LivingEntity, Integer, Runnable>> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            Triple<LivingEntity, Integer, Runnable> task = iterator.next();
            int remaining = task.getMiddle() - 1;

            if (remaining <= 0) {
                try {
                    task.getRight().run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                iterator.remove();
            } else {
                task.setMiddle(remaining);
            }
        }
    }
    private static class Triple<L, M, R> {
        private final L left;
        private M middle;
        private final R right;

        public Triple(L left, M middle, R right) {
            this.left = left;
            this.middle = middle;
            this.right = right;
        }

        public L getLeft() { return left; }
        public M getMiddle() { return middle; }
        public void setMiddle(M middle) { this.middle = middle; }
        public R getRight() { return right; }
    }
}
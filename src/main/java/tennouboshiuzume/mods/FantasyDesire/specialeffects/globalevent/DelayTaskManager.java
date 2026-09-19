package tennouboshiuzume.mods.FantasyDesire.specialeffects.globalevent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// 待重制 已不使用 为什么forge不提供一个延时代码块功能？导致所有作者都要自己造个轮子
@Mod.EventBusSubscriber
public class DelayTaskManager {
    private static final List<Triple<LivingEntity, Integer, Runnable>> TASKS = new ArrayList<>();

    public static void add(LivingEntity entity, int delayTick, Runnable action) {
        TASKS.add(new Triple<>(entity, delayTick, action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
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

        public L getLeft() {
            return left;
        }

        public M getMiddle() {
            return middle;
        }

        public void setMiddle(M middle) {
            this.middle = middle;
        }

        public R getRight() {
            return right;
        }
    }
}
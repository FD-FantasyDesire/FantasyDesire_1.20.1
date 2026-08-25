package tennouboshiuzume.mods.FantasyDesire.client.text;

import com.mojang.datafixers.util.Either;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextContents;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RichTextTooltipHandler {
    private static final long RESET_AFTER_MILLIS = 150L;
    private static final Map<String, AnimationState> ANIMATIONS = new HashMap<>();

    private RichTextTooltipHandler() {
    }

    @SubscribeEvent
    public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        if (!(event.getItemStack().getItem() instanceof ItemFantasySlashBlade)) {
            return;
        }

        long now = Util.getMillis();
        int stackIdentity = System.identityHashCode(event.getItemStack());
        RichTextContents richTitle = null;
        if (!event.getItemStack().hasCustomHoverName()) {
            MutableComponent title = RichTextClient.translatable(event.getItemStack().getDescriptionId());
            if (title.getContents() instanceof RichTextContents contents) {
                richTitle = contents;
            }
        }
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        ListIterator<Either<FormattedText, TooltipComponent>> iterator = elements.listIterator();
        int lineIndex = 0;
        while (iterator.hasNext()) {
            Either<FormattedText, TooltipComponent> element = iterator.next();
            FormattedText formatted = element.left().orElse(null);
            RichTextContents rich = lineIndex == 0 ? richTitle : null;
            lineIndex++;
            if (rich == null && formatted instanceof Component component
                    && component.getContents() instanceof RichTextContents contents) {
                rich = contents;
            }
            if (rich == null || !rich.document().hasEffects()) {
                continue;
            }

            String animationKey = stackIdentity + ":" + rich.document().sourceId();
            AnimationState state = ANIMATIONS.get(animationKey);
            if (state == null || now - state.lastSeen() > RESET_AFTER_MILLIS) {
                state = new AnimationState(now, now);
            } else {
                state = new AnimationState(state.startedAt(), now);
            }
            ANIMATIONS.put(animationKey, state);
            iterator.set(Either.right(new RichTooltipComponent(rich.document(), state.startedAt())));
        }

        if (ANIMATIONS.size() > 256) {
            ANIMATIONS.entrySet().removeIf(entry -> now - entry.getValue().lastSeen() > 1000L);
        }
    }

    private record AnimationState(long startedAt, long lastSeen) {
    }
}

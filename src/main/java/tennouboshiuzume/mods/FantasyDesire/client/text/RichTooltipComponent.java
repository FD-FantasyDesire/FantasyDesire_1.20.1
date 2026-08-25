package tennouboshiuzume.mods.FantasyDesire.client.text;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;

public record RichTooltipComponent(RichTextDocument document, long startedAtMillis) implements TooltipComponent {
}

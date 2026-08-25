package tennouboshiuzume.mods.FantasyDesire.textutils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/** 将新文档降级为原版 Component，供非自定义渲染场景使用。 */
public final class TextRenderer {
    private TextRenderer() {
    }

    public static MutableComponent render(RichTextDocument document, double globalTicks, double ageTicks) {
        MutableComponent result = Component.empty();
        for (RichTextDocument.Glyph glyph : document.glyphs()) {
            GlyphVisual visual = TextEffectEngine.evaluateMain(glyph, globalTicks, ageTicks);
            if (!visual.visible()) {
                continue;
            }
            MutableComponent character = Component.literal(new String(Character.toChars(glyph.codePoint())));
            character.setStyle(visual.style().withColor(TextColor.fromRgb(visual.color())));
            result.append(character);
        }
        return result;
    }
}

package tennouboshiuzume.mods.FantasyDesire.textutils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/** 仅用于客户端 Tooltip 构造；普通渲染路径会访问其中的原版 Component 回退。 */
public final class RichTextContents implements ComponentContents {
    private final RichTextDocument document;
    private final Component fallback;

    public RichTextContents(RichTextDocument document, Component fallback) {
        this.document = document;
        this.fallback = fallback;
    }

    public RichTextDocument document() {
        return document;
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> consumer, Style style) {
        return fallback.visit(consumer, style);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> consumer) {
        return fallback.visit(consumer);
    }

    @Override
    public String toString() {
        return "rich_text{" + document.sourceId() + '}';
    }
}

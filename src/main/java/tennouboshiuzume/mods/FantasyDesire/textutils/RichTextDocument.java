package tennouboshiuzume.mods.FantasyDesire.textutils;

import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/** 解析完成后与渲染环境无关的富文本文档。 */
public final class RichTextDocument {
    public sealed interface Node permits Text, Effect {
    }

    public record Text(String value, Style style) implements Node {
    }

    public record Effect(TextEffectSpec spec, List<Node> children) implements Node {
        public Effect {
            children = List.copyOf(children);
        }
    }

    public record EffectRef(TextEffectSpec spec, int scopeId, int localIndex, int length) {
    }

    public record Glyph(int codePoint, Style style, int index, List<EffectRef> effects) {
        public Glyph {
            effects = List.copyOf(effects);
        }
    }

    private final String sourceId;
    private final List<Node> roots;
    private final List<Glyph> glyphs;

    public RichTextDocument(String sourceId, List<Node> roots) {
        this.sourceId = sourceId;
        this.roots = List.copyOf(roots);
        List<Glyph> flattened = flattenNodes(this.roots, new int[] { 0 });
        List<Glyph> indexed = new ArrayList<>(flattened.size());
        for (int i = 0; i < flattened.size(); i++) {
            Glyph glyph = flattened.get(i);
            indexed.add(new Glyph(glyph.codePoint(), glyph.style(), i, glyph.effects()));
        }
        this.glyphs = List.copyOf(indexed);
    }

    public String sourceId() {
        return sourceId;
    }

    public List<Node> roots() {
        return roots;
    }

    public List<Glyph> glyphs() {
        return glyphs;
    }

    public boolean hasEffects() {
        return glyphs.stream().anyMatch(glyph -> !glyph.effects().isEmpty());
    }

    private static List<Glyph> flattenNodes(List<Node> nodes, int[] nextScopeId) {
        List<Glyph> result = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof Text text) {
                text.value().codePoints().forEach(codePoint ->
                        result.add(new Glyph(codePoint, text.style(), -1, List.of())));
            } else if (node instanceof Effect effect) {
                int scopeId = nextScopeId[0]++;
                List<Glyph> children = flattenNodes(effect.children(), nextScopeId);
                int length = children.size();
                for (int i = 0; i < length; i++) {
                    Glyph child = children.get(i);
                    List<EffectRef> effects = new ArrayList<>(child.effects().size() + 1);
                    effects.add(new EffectRef(effect.spec(), scopeId, i, length));
                    effects.addAll(child.effects());
                    result.add(new Glyph(child.codePoint(), child.style(), -1, effects));
                }
            }
        }
        return result;
    }
}

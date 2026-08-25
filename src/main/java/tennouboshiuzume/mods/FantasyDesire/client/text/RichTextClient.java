package tennouboshiuzume.mods.FantasyDesire.client.text;

import net.minecraft.Util;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextContents;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextParser;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextRenderer;

import java.util.HashMap;
import java.util.Map;

/** 客户端本地化字符串到富文本组件的统一入口。 */
public final class RichTextClient {
    private static final int MAX_CACHE_SIZE = 512;
    private static final Map<CacheKey, RichTextDocument> CACHE = new HashMap<>();

    private RichTextClient() {
    }

    public static String rawTranslation(String key) {
        return I18n.get(key);
    }

    public static MutableComponent translatable(String key) {
        return fromRaw(key, I18n.get(key), Style.EMPTY);
    }

    public static MutableComponent fallbackTranslatable(String key) {
        RichTextDocument document = document(key, I18n.get(key), Style.EMPTY);
        double globalTicks = Util.getMillis() / 50.0;
        return TextRenderer.render(document, globalTicks, Double.MAX_VALUE);
    }

    public static MutableComponent formatted(String sourceId, String translationKey, Style baseStyle, Object... args) {
        return fromRaw(sourceId, I18n.get(translationKey, args), baseStyle);
    }

    public static MutableComponent fromRaw(String sourceId, String value, Style baseStyle) {
        RichTextDocument document = document(sourceId, value, baseStyle);
        double globalTicks = Util.getMillis() / 50.0;
        MutableComponent fallback = TextRenderer.render(document, globalTicks, Double.MAX_VALUE);
        return MutableComponent.create(new RichTextContents(document, fallback));
    }

    private static synchronized RichTextDocument document(String sourceId, String value, Style baseStyle) {
        CacheKey key = new CacheKey(sourceId, value, baseStyle);
        RichTextDocument cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (CACHE.size() >= MAX_CACHE_SIZE) {
            CACHE.clear();
        }
        RichTextDocument parsed = new TextParser().parse(sourceId, value, baseStyle);
        CACHE.put(key, parsed);
        return parsed;
    }

    private record CacheKey(String sourceId, String value, Style baseStyle) {
    }
}

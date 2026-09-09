package tennouboshiuzume.mods.FantasyDesire.textutils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 类 HTML 富文本解析器，支持静态样式、可组合效果和旧版 Style 动画标签。 */
public final class TextParser {
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "([A-Za-z][A-Za-z0-9_-]*)\\s*=\\s*\"([^\"]*)\"");
    private static final Set<String> REPORTED_WARNINGS = new HashSet<>();
    private static final int MAX_DEPTH = 16;
    private static final int MAX_COLORS = 16;

    public RichTextDocument parse(String input) {
        return parse("inline", input, Style.EMPTY);
    }

    public RichTextDocument parse(String sourceId, String input) {
        return parse(sourceId, input, Style.EMPTY);
    }

    public RichTextDocument parse(String sourceId, String input, Style baseStyle) {
        String value = input == null ? "" : input;
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame("root", baseStyle, null));

        int cursor = 0;
        while (cursor < value.length()) {
            int opening = value.indexOf('<', cursor);
            if (opening < 0) {
                appendText(stack.peek(), value.substring(cursor));
                break;
            }
            if (opening > cursor) {
                appendText(stack.peek(), value.substring(cursor, opening));
            }

            int closing = findTagEnd(value, opening + 1);
            if (closing < 0) {
                appendText(stack.peek(), value.substring(opening));
                warn(sourceId, "存在未闭合的标签起始符");
                break;
            }

            String token = value.substring(opening + 1, closing).trim();
            if (!handleTag(sourceId, token, stack)) {
                appendText(stack.peek(), value.substring(opening, closing + 1));
            }
            cursor = closing + 1;
        }

        while (stack.size() > 1) {
            Frame frame = stack.pop();
            warn(sourceId, "标签 <" + frame.tag + "> 未闭合");
            finishFrame(stack.peek(), frame);
        }
        return new RichTextDocument(sourceId, stack.pop().children);
    }

    private boolean handleTag(String sourceId, String token, Deque<Frame> stack) {
        if (token.isEmpty()) {
            return false;
        }
        boolean closing = token.startsWith("/");
        String body = closing ? token.substring(1).trim() : token;
        int separator = firstWhitespace(body);
        String tag = (separator < 0 ? body : body.substring(0, separator)).toLowerCase(Locale.ROOT);
        String attributeText = separator < 0 ? "" : body.substring(separator + 1);

        if (closing) {
            if (stack.size() <= 1 || !stack.peek().tag.equals(tag)) {
                warn(sourceId, "结束标签 </" + tag + "> 与当前层级不匹配");
                return false;
            }
            Frame frame = stack.pop();
            finishFrame(stack.peek(), frame);
            return true;
        }

        if (!tag.equals("style") && !tag.equals("fx")) {
            warn(sourceId, "未知标签 <" + tag + ">，已按普通文本处理");
            return false;
        }
        if (stack.size() >= MAX_DEPTH) {
            warn(sourceId, "标签嵌套超过 " + MAX_DEPTH + " 层");
            return false;
        }

        Map<String, String> attributes = parseAttributes(sourceId, attributeText);
        Style inherited = stack.peek().currentStyle;
        if (tag.equals("style")) {
            stack.push(new Frame(tag, applyStyle(sourceId, inherited, attributes), null));
        } else {
            TextEffectSpec effect = parseEffect(sourceId, attributes);
            stack.push(new Frame(tag, inherited, effect));
        }
        return true;
    }

    private static void finishFrame(Frame parent, Frame frame) {
        if (frame.effect == null) {
            parent.children.addAll(frame.children);
        } else {
            parent.children.add(new RichTextDocument.Effect(frame.effect, frame.children));
        }
    }

    private Map<String, String> parseAttributes(String sourceId, String input) {
        Map<String, String> attributes = new HashMap<>();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(input);
        int consumed = 0;
        while (matcher.find()) {
            if (!input.substring(consumed, matcher.start()).isBlank()) {
                warn(sourceId, "无法解析的标签属性: " + input.substring(consumed, matcher.start()).trim());
            }
            attributes.put(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2));
            consumed = matcher.end();
        }
        if (!input.substring(consumed).isBlank()) {
            warn(sourceId, "无法解析的标签属性: " + input.substring(consumed).trim());
        }
        return attributes;
    }

    private Style applyStyle(String sourceId, Style inherited, Map<String, String> attributes) {
        Style style = inherited;
        if (attributes.containsKey("color")) {
            style = style.withColor(TextColor.fromRgb(parseRgb(sourceId, attributes.get("color"), 0xFFFFFF)));
        }
        if (attributes.containsKey("bold")) {
            style = style.withBold(booleanValue(attributes.get("bold"), false));
        }
        if (attributes.containsKey("italic")) {
            style = style.withItalic(booleanValue(attributes.get("italic"), false));
        }
        if (attributes.containsKey("underlined") || attributes.containsKey("underline")) {
            style = style.withUnderlined(booleanValue(attributes.getOrDefault("underlined",
                    attributes.get("underline")), false));
        }
        if (attributes.containsKey("strikethrough") || attributes.containsKey("strike")) {
            style = style.withStrikethrough(booleanValue(attributes.getOrDefault("strikethrough",
                    attributes.get("strike")), false));
        }
        if (attributes.containsKey("obfuscated")) {
            style = style.withObfuscated(booleanValue(attributes.get("obfuscated"), false));
        }
        if (attributes.containsKey("font")) {
            try {
                style = style.withFont(new ResourceLocation(attributes.get("font")));
            } catch (RuntimeException exception) {
                warn(sourceId, "非法字体 ID: " + attributes.get("font"));
            }
        }
        return style;
    }

    private TextEffectSpec parseEffect(String sourceId, Map<String, String> attributes) {
        String id = attributes.getOrDefault("id", "none").toLowerCase(Locale.ROOT)
                .replace('-', '_');
        int namespace = id.indexOf(':');
        if (namespace >= 0) {
            id = id.substring(namespace + 1);
        }

        return switch (id) {
            case "gradient", "dynamic_gradient" -> new TextEffectSpec.Gradient(
                    parseColors(sourceId, attributes.getOrDefault("colors",
                            attributes.getOrDefault("color", "#FFFFFF,#FFFFFF"))),
                    positiveFloat(sourceId, attributes, "period", 50.0F),
                    booleanValue(attributes.getOrDefault("animated", "true"), true));
            case "static_gradient" -> new TextEffectSpec.Gradient(
                    parseColors(sourceId, attributes.getOrDefault("colors",
                            attributes.getOrDefault("color", "#FFFFFF,#FFFFFF"))),
                    1.0F, false);
            case "rainbow" -> new TextEffectSpec.Rainbow(
                    positiveFloat(sourceId, attributes, "period", 50.0F),
                    positiveFloat(sourceId, attributes, "spread", 10.0F));
            case "wave_bold" -> new TextEffectSpec.WaveBold(
                    positiveFloat(sourceId, attributes, "step", 2.0F));
            case "wave_slide" -> new TextEffectSpec.WaveSlide(
                    positiveFloat(sourceId, attributes, "step", 2.0F),
                    positiveInt(sourceId, attributes, "width", 2));
            case "shadow" -> new TextEffectSpec.Shadow(
                    parseArgb(sourceId, attributes.getOrDefault("color", "#A0000000"), 0xA0000000),
                    finiteFloat(sourceId, attributes, "offset-x", 1.0F),
                    finiteFloat(sourceId, attributes, "offset-y", 1.0F));
            case "outline" -> new TextEffectSpec.Outline(
                    parseArgb(sourceId, attributes.getOrDefault("color", "#A0000000"), 0xA0000000),
                    finiteFloat(sourceId, attributes, "offset-x", 1.0F),
                    finiteFloat(sourceId, attributes, "offset-y", 1.0F));
            case "shake" -> new TextEffectSpec.Shake(
                    nonNegativeFloat(sourceId, attributes, "amplitude-x", 0.5F),
                    nonNegativeFloat(sourceId, attributes, "amplitude-y", 0.5F),
                    positiveFloat(sourceId, attributes, "period", 2.0F),
                    intValue(sourceId, attributes, "seed", 0));
            case "glitch" -> new TextEffectSpec.Glitch(
                    nonNegativeFloat(sourceId, attributes, "amplitude", 2.0F),
                    positiveFloat(sourceId, attributes, "slice-height", 1.0F),
                    positiveFloat(sourceId, attributes, "period", 2.0F),
                    unitFloat(sourceId, attributes, "chance", 0.2F),
                    intValue(sourceId, attributes, "seed", 0));
            case "wave" -> new TextEffectSpec.Wave(
                    nonNegativeFloat(sourceId, attributes, "amplitude", 1.5F),
                    positiveFloat(sourceId, attributes, "wavelength", 6.0F),
                    positiveFloat(sourceId, attributes, "period", 20.0F));
            case "typewriter" -> new TextEffectSpec.Typewriter(
                    positiveFloat(sourceId, attributes, "cps", 12.0F),
                    nonNegativeFloat(sourceId, attributes, "delay", 0.0F),
                    nonNegativeFloat(sourceId, attributes, "fade", 2.0F),
                    booleanValue(attributes.getOrDefault("reserve", "true"), true));
            case "none" -> null;
            default -> {
                warn(sourceId, "未知文字效果: " + id);
                yield null;
            }
        };
    }

    private List<Integer> parseColors(String sourceId, String value) {
        String[] values = value.split(",");
        List<Integer> colors = new ArrayList<>();
        for (String color : values) {
            if (colors.size() >= MAX_COLORS) {
                warn(sourceId, "渐变颜色数量超过 " + MAX_COLORS + "，多余颜色已忽略");
                break;
            }
            colors.add(parseRgb(sourceId, color.trim(), 0xFFFFFF));
        }
        if (colors.isEmpty()) {
            colors.add(0xFFFFFF);
        }
        return colors;
    }

    private void appendText(Frame frame, String input) {
        if (input.isEmpty()) {
            return;
        }
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (character == '§' && i + 1 < input.length()) {
                flushText(frame, buffer);
                ChatFormatting formatting = ChatFormatting.getByCode(input.charAt(++i));
                if (formatting == null) {
                    buffer.append('§').append(input.charAt(i));
                } else if (formatting == ChatFormatting.RESET) {
                    frame.currentStyle = frame.baseStyle;
                } else if (formatting.isColor()) {
                    frame.currentStyle = frame.baseStyle.applyFormat(formatting);
                } else {
                    frame.currentStyle = frame.currentStyle.applyFormat(formatting);
                }
            } else {
                buffer.append(character);
            }
        }
        flushText(frame, buffer);
    }

    private static void flushText(Frame frame, StringBuilder buffer) {
        if (buffer.length() > 0) {
            String value = buffer.toString()
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&");
            frame.children.add(new RichTextDocument.Text(value, frame.currentStyle));
            buffer.setLength(0);
        }
    }

    private static int findTagEnd(String value, int start) {
        boolean quoted = false;
        for (int i = start; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '"') {
                quoted = !quoted;
            } else if (character == '>' && !quoted) {
                return i;
            }
        }
        return -1;
    }

    private static int firstWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private float positiveFloat(String sourceId, Map<String, String> attributes, String key, float fallback) {
        return Math.max(0.05F, finiteFloat(sourceId, attributes, key, fallback));
    }

    private float nonNegativeFloat(String sourceId, Map<String, String> attributes, String key, float fallback) {
        return Math.max(0.0F, finiteFloat(sourceId, attributes, key, fallback));
    }

    private float unitFloat(String sourceId, Map<String, String> attributes, String key, float fallback) {
        return Math.max(0.0F, Math.min(1.0F, finiteFloat(sourceId, attributes, key, fallback)));
    }

    private float finiteFloat(String sourceId, Map<String, String> attributes, String key, float fallback) {
        String value = attributes.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            float parsed = Float.parseFloat(value);
            if (Float.isFinite(parsed)) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }
        warn(sourceId, "属性 " + key + " 不是有限数字: " + value);
        return fallback;
    }

    private int positiveInt(String sourceId, Map<String, String> attributes, String key, int fallback) {
        return Math.max(1, intValue(sourceId, attributes, key, fallback));
    }

    private int intValue(String sourceId, Map<String, String> attributes, String key, int fallback) {
        String value = attributes.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            warn(sourceId, "属性 " + key + " 不是整数: " + value);
            return fallback;
        }
    }

    private int parseRgb(String sourceId, String value, int fallback) {
        return parseColor(sourceId, value, fallback) & 0xFFFFFF;
    }

    private int parseArgb(String sourceId, String value, int fallback) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() == 6) {
            return 0xFF000000 | parseColor(sourceId, normalized, fallback);
        }
        return parseColor(sourceId, normalized, fallback);
    }

    private int parseColor(String sourceId, String value, int fallback) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        try {
            if (normalized.length() != 6 && normalized.length() != 8) {
                throw new NumberFormatException();
            }
            return (int) Long.parseLong(normalized, 16);
        } catch (NumberFormatException ignored) {
            warn(sourceId, "非法颜色值: " + value);
            return fallback;
        }
    }

    private static boolean booleanValue(String value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        return fallback;
    }

    private static synchronized void warn(String sourceId, String message) {
        String key = sourceId + ':' + message;
        if (REPORTED_WARNINGS.add(key)) {
            System.err.println("[FantasyDesire/RichText] " + sourceId + ": " + message);
        }
    }

    private static final class Frame {
        private final String tag;
        private final Style baseStyle;
        private final TextEffectSpec effect;
        private final List<RichTextDocument.Node> children = new ArrayList<>();
        private Style currentStyle;

        private Frame(String tag, Style baseStyle, TextEffectSpec effect) {
            this.tag = tag;
            this.baseStyle = baseStyle;
            this.currentStyle = baseStyle;
            this.effect = effect;
        }
    }
}

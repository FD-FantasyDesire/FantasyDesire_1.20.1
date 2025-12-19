package tennouboshiuzume.mods.FantasyDesire.TextUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParser {
    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)(\\w+)(?:\\s+([^>]+))?>");

    public List<TextNode> parseMultipleTrees(String input) {
        List<TextNode> roots = new ArrayList<>();
        Deque<TextNode> stack = new ArrayDeque<>();
        int lastIndex = 0;

        Matcher matcher = TAG_PATTERN.matcher(input);
        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                String text = input.substring(lastIndex, matcher.start());
                addNode(stack, roots, new TextLeaf(parseLegacyFormatting(text)));
            }

            String slash = matcher.group(1);
            String tag = matcher.group(2).toLowerCase();
            String param = matcher.group(3);

            if (slash.isEmpty()) {
                TextNode node = createNode(tag, param);
                stack.push(node);
            } else {
                if (!stack.isEmpty()) {
                    TextNode node = stack.pop();
                    addNode(stack, roots, node);
                }
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < input.length()) {
            String text = input.substring(lastIndex);
            addNode(stack, roots, new TextLeaf(parseLegacyFormatting(text)));
        }

        while (!stack.isEmpty()) roots.add(stack.pop());
        return roots;
    }
    public static MutableComponent parseLegacyFormatting(String s) {
        MutableComponent result = Component.literal("");
        if (s == null || s.isEmpty()) return result;

        Style current = Style.EMPTY;
        StringBuilder buf = new StringBuilder();
        boolean expectingCode = false;

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (expectingCode) {
                expectingCode = false;

                Style newStyle = current;

                switch (Character.toLowerCase(ch)) {
                    case 'k': newStyle = newStyle.withObfuscated(true); break;
                    case 'l': newStyle = newStyle.withBold(true); break;
                    case 'm': newStyle = newStyle.withStrikethrough(true); break;
                    case 'n': newStyle = newStyle.withUnderlined(true); break;
                    case 'o': newStyle = newStyle.withItalic(true); break;
                    case 'r': newStyle = Style.EMPTY; break;

                    case '0': newStyle = Style.EMPTY.withColor(0x000000); break;
                    case '1': newStyle = Style.EMPTY.withColor(0x0000AA); break;
                    case '2': newStyle = Style.EMPTY.withColor(0x00AA00); break;
                    case '3': newStyle = Style.EMPTY.withColor(0x00AAAA); break;
                    case '4': newStyle = Style.EMPTY.withColor(0xAA0000); break;
                    case '5': newStyle = Style.EMPTY.withColor(0xAA00AA); break;
                    case '6': newStyle = Style.EMPTY.withColor(0xFFAA00); break;
                    case '7': newStyle = Style.EMPTY.withColor(0xAAAAAA); break;
                    case '8': newStyle = Style.EMPTY.withColor(0x555555); break;
                    case '9': newStyle = Style.EMPTY.withColor(0x5555FF); break;
                    case 'a': newStyle = Style.EMPTY.withColor(0x55FF55); break;
                    case 'b': newStyle = Style.EMPTY.withColor(0x55FFFF); break;
                    case 'c': newStyle = Style.EMPTY.withColor(0xFF5555); break;
                    case 'd': newStyle = Style.EMPTY.withColor(0xFF55FF); break;
                    case 'e': newStyle = Style.EMPTY.withColor(0xFFFF55); break;
                    case 'f': newStyle = Style.EMPTY.withColor(0xFFFFFF); break;

                    default:
                        // 不是合法格式 → 原样输出 "§x"
                        buf.append('§').append(ch);
                        continue;
                }

                // 先把旧 buffer 输出成旧 style
                if (buf.length() > 0) {
                    result.append(Component.literal(buf.toString()).setStyle(current));
                    buf.setLength(0);
                }

                current = newStyle;
                continue;
            }

            if (ch == '§') {
                expectingCode = true;

                // flush 当前 buffer
                if (buf.length() > 0) {
                    result.append(Component.literal(buf.toString()).setStyle(current));
                    buf.setLength(0);
                }
                continue;
            }

            buf.append(ch);
        }

        // flush 最后一段
        if (buf.length() > 0) {
            result.append(Component.literal(buf.toString()).setStyle(current));
        }

        return result;
    }


    private void addNode(Deque<TextNode> stack, List<TextNode> roots, TextNode node) {
        if (!stack.isEmpty()) stack.peek().children.add(node);
        else roots.add(node);
    }

    private TextNode createNode(String tag, String param) {
        if (!"style".equals(tag)) return new TextLeaf(Component.literal("")); // 未知标签当作空

        String type = "None";
        long speed = 5;
        TextColor[] colors = new TextColor[]{TextColor.fromRgb(0xFFFFFF)};

        if (param != null) {
            Matcher typeMatcher = Pattern.compile("Type\\s*=\\s*\"(\\w+)\"").matcher(param);
            if (typeMatcher.find()) type = typeMatcher.group(1);

            Matcher speedMatcher = Pattern.compile("Speed\\s*=\\s*\"?(\\d+)\"?").matcher(param);
            if (speedMatcher.find()) speed = Long.parseLong(speedMatcher.group(1));

            Matcher colorMatcher = Pattern.compile("Color\\s*=\\s*(\"[^\"]+\"(,\"[^\"]+\")*)").matcher(param);
            if (colorMatcher.find()) {
                String[] cs = colorMatcher.group(1).replace("\"","").split(",");
                List<TextColor> list = new ArrayList<>();
                for (String c : cs) {
                    try { list.add(TextColor.fromRgb(Integer.parseInt(c.replace("#",""),16))); }
                    catch(Exception ignored) {}
                }
                if (!list.isEmpty()) colors = list.toArray(new TextColor[0]);
            }
        }

        return new StyleNode(type, colors, speed);
    }
}

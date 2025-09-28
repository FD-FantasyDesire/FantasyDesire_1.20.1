package tennouboshiuzume.mods.FantasyDesire.TextUtils;

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
                addNode(stack, roots, new TextLeaf(text));
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
            addNode(stack, roots, new TextLeaf(text));
        }

        while (!stack.isEmpty()) roots.add(stack.pop());
        return roots;
    }

    private void addNode(Deque<TextNode> stack, List<TextNode> roots, TextNode node) {
        if (!stack.isEmpty()) stack.peek().children.add(node);
        else roots.add(node);
    }

    private TextNode createNode(String tag, String param) {
        if (!"style".equals(tag)) return new TextLeaf(""); // 未知标签当作空

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

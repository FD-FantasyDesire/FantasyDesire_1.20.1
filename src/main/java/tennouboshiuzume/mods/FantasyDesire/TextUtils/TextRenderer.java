package tennouboshiuzume.mods.FantasyDesire.TextUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim.*;

import java.util.List;

public class TextRenderer {
    public static MutableComponent render(List<TextNode> roots, long tick) {
        MutableComponent result = Component.literal("");
        for (TextNode root : roots) {
            result.append(renderNode(root, tick, null));
        }
        return result;
    }

    private static MutableComponent renderNode(TextNode node, long tick, StyleNode parentStyle) {
        MutableComponent result = Component.literal("");

        StyleNode currentStyle = parentStyle;
        if (node instanceof StyleNode styleNode) {
            currentStyle = styleNode;
        }

        if (node instanceof TextLeaf leaf) {
            // 直接拼接叶子文本
            result.append(Component.literal(leaf.content));
        } else {
            // 遍历子节点
            for (TextNode child : node.children) {
                result.append(renderNode(child, tick, currentStyle));
            }
        }

        // 应用动画
        if (currentStyle != null) {
            ITextAnim anim = switch (currentStyle.type.toLowerCase()) {
                case "rainbow" -> new RainbowAnim();
                case "wavebold" -> new WaveBoldAnim();
                case "waveslide" -> new WaveSlideAnim();
                case "staticgradient" -> new StaticGradientAnim();
                case "dynamicgradient" -> new DynamicGradientAnim();
                default -> (c, t, s, col) -> c; // 默认不动画
            };
            result = anim.apply(result, tick, currentStyle.speed, currentStyle.colors);
        }

        return result;
    }
}

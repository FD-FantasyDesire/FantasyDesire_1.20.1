package tennouboshiuzume.mods.FantasyDesire.TextUtils;

import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

//文本节点
public abstract class TextNode {
    List<TextNode> children = new ArrayList<>();
}

// 文本叶子
class TextLeaf extends TextNode {
    String content;
    TextLeaf(String content) { this.content = content; }
}

// 样式节点，整合颜色和动画
class StyleNode extends TextNode {
    String type;           // 动画类型，例如 "Rainbow", "Blink" 等
    TextColor[] colors;    // 颜色数组
    long speed;            // 动画速度（帧间隔）
    StyleNode(String type, TextColor[] colors, long speed) {
        this.type = type;
        this.colors = colors;
        this.speed = speed;
    }
}
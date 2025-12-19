package tennouboshiuzume.mods.FantasyDesire.TextUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class ComponentUtils {
    public interface CharVisitor {
        MutableComponent visit(char c, Style style, int index);
    }

    public static MutableComponent forEachChar(Component component, CharVisitor visitor) {
        MutableComponent result = Component.literal("");// 展开的纯文本
        int index = 0;
        for (Component child : component.toFlatList()) { // Forge 的 helper，可以展平 siblings
            String part = child.getString();
            Style style = child.getStyle();
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                result.append(visitor.visit(c, style, index));
                index++;
            }
        }
        return result;
    }
}


package tennouboshiuzume.mods.FantasyDesire.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class StyleData {
    public boolean bold, italic, underlined, strikethrough;
    public List<Integer> colors;   // 单色或多色数组
    public StyleData copy() {
        StyleData s = new StyleData();
        s.bold = this.bold; s.italic = this.italic;
        s.underlined = this.underlined; s.strikethrough = this.strikethrough;
        if (this.colors != null) s.colors = new ArrayList<>(this.colors);
        return s;
    }
}

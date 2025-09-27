package tennouboshiuzume.mods.FantasyDesire.TextUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim.DynamicGradientAnimation;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim.RainbowAnimation;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim.StaticGradientAnimation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// =========================================
// 解析器主类
// =========================================
public class TagTextParser {

    private static final Pattern TAG_PATTERN = Pattern.compile("<(/)?([a-zA-Z]+)([^>]*)>");
    private static final Pattern ATTR_PATTERN = Pattern.compile("([a-zA-Z0-9_-]+)\\s*=\\s*\"([^\"]*)\"|([a-zA-Z0-9_-]+)");

    // 外部调用
    public static MutableComponent parseWithTick(String raw, long tick) {
        if (!raw.contains("<FancyStyle>")) return Component.literal(raw); // 快速过滤

        Node root = parseToNodes(raw);
        return nodeToComponent(root, tick, new StyleData());
    }

    // ================== AST ==================
    private abstract static class Node { }
    private static class TextNode extends Node { final String text; TextNode(String t){this.text=t;} }
    private static class TagNode extends Node {
        final String tag;
        final Map<String,String> attrs;
        final List<Node> children = new ArrayList<>();
        TagNode(String tag, Map<String,String> attrs){this.tag=tag.toLowerCase(Locale.ROOT); this.attrs=attrs;}
    }

    // ================== 解析 ==================
    private static Node parseToNodes(String raw) {
        Deque<TagNode> stack = new ArrayDeque<>();
        TagNode root = new TagNode("__root__", Collections.emptyMap());
        stack.push(root);

        Matcher m = TAG_PATTERN.matcher(raw);
        int last = 0;
        while (m.find()) {
            if (m.start()>last) stack.peek().children.add(new TextNode(raw.substring(last,m.start())));
            boolean closing = m.group(1)!=null;
            String tag = m.group(2);
            String attrsRaw = m.group(3).trim();
            Map<String,String> attrs = parseAttrs(attrsRaw);

            if(closing){
                while(!stack.isEmpty()){
                    TagNode top=stack.pop();
                    if(top.tag.equalsIgnoreCase(tag)) break;
                }
            } else {
                if(tag.equalsIgnoreCase("color") && attrsRaw.startsWith("=")){
                    String val=attrsRaw.substring(1).trim();
                    attrs=Map.of("value", val);
                }
                TagNode node = new TagNode(tag, attrs);
                stack.peek().children.add(node);
                stack.push(node);
            }
            last = m.end();
        }
        if(last<raw.length()) stack.peek().children.add(new TextNode(raw.substring(last)));
        return root;
    }

    private static Map<String,String> parseAttrs(String raw){
        Map<String,String> map=new HashMap<>();
        if(raw==null||raw.isEmpty()) return map;
        Matcher m=ATTR_PATTERN.matcher(raw);
        while(m.find()){
            if(m.group(1)!=null) map.put(m.group(1).toLowerCase(Locale.ROOT), m.group(2));
            else if(m.group(3)!=null) map.put(m.group(3).toLowerCase(Locale.ROOT), "");
        }
        return map;
    }

    // ================== 递归生成 Component ==================
    private static MutableComponent nodeToComponent(Node node,long tick,StyleData current){
        MutableComponent out=Component.literal("");
        if(node instanceof TextNode t){
            out.append(applyStyleToText(t.text,current));
        }else if(node instanceof TagNode tn){
            if(tn.tag.equals("__root__")){
                for(Node c:tn.children) out.append(nodeToComponent(c,tick,current));
                return out;
            }
            StyleData next=current.copy();
            switch (tn.tag){
                case "b": next.bold=true; break;
                case "i": next.italic=true; break;
                case "u": next.underlined=true; break;
                case "s": next.strikethrough=true; break;
                case "color":
                    if(tn.attrs.containsKey("value")){
                        String val=tn.attrs.get("value");
                        List<Integer> cols=parseColors(val);
                        next.colors=cols;
                    }
                    break;
            }

            if(tn.tag.equals("anim")){
                String type = tn.attrs.getOrDefault("type","rainbow").toLowerCase(Locale.ROOT);
                double speed = 0.2;
                if(tn.attrs.containsKey("speed")) speed = Double.parseDouble(tn.attrs.get("speed"));
                ITextAnimation anim;
                switch(type){
                    case "static_gradient": anim = new StaticGradientAnimation(); break;
                    case "dynamic_gradient": anim = new DynamicGradientAnimation(speed); break;
                    case "rainbow": anim = new RainbowAnimation(speed); break;
                    default: anim = new StaticGradientAnimation(); break;
                }
                for(Node c:tn.children){
                    MutableComponent childComp = nodeToComponent(c,tick,next.copy());
                    childComp = anim.apply(childComp.getString(), next, tick);
                    out.append(childComp);
                }
            }else{
                for(Node c:tn.children) out.append(nodeToComponent(c,tick,next));
            }
        }
        return out;
    }

    // ================== 样式应用 ==================
    public static MutableComponent applyStyleToText(String text,StyleData style){
        MutableComponent c=Component.literal(text);
        Style s=Style.EMPTY;
        if(style.bold) s=s.withBold(true);
        if(style.italic) s=s.withItalic(true);
        if(style.underlined) s=s.withUnderlined(true);
        if(style.strikethrough) s=s.withStrikethrough(true);
        if(style.colors!=null && !style.colors.isEmpty()){
            int rgb = style.colors.get(0);
            s=s.withColor(TextColor.fromRgb(rgb));
        }
        c.setStyle(s);
        return c;
    }

    // ================== 辅助函数 ==================
    public static int lerpColor(int c1,int c2,double t){
        int r1=(c1>>16)&0xFF, g1=(c1>>8)&0xFF, b1=c1&0xFF;
        int r2=(c2>>16)&0xFF, g2=(c2>>8)&0xFF, b2=c2&0xFF;
        int r=(int)(r1*(1-t)+r2*t);
        int g=(int)(g1*(1-t)+g2*t);
        int b=(int)(b1*(1-t)+b2*t);
        return (r<<16)|(g<<8)|b;
    }

    public static int hsbToRgb(float h,float s,float b){
        return java.awt.Color.HSBtoRGB(h,s,b) & 0xFFFFFF;
    }

    private static List<Integer> parseColors(String raw){
        List<Integer> list=new ArrayList<>();
        String[] parts=raw.split(",");
        for(String p:parts){
            Integer c=parseColorHex(p.trim());
            if(c!=null) list.add(c);
        }
        return list.isEmpty()?null:list;
    }

    private static Integer parseColorHex(String raw){
        if(raw==null) return null;
        raw=raw.trim();
        if(raw.startsWith("#")) raw=raw.substring(1);
        try{return Integer.parseInt(raw,16)&0xFFFFFF;}catch(Exception e){return null;}
    }
}

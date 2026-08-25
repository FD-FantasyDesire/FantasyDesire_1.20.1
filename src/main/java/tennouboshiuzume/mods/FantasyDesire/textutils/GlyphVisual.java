package tennouboshiuzume.mods.FantasyDesire.textutils;

import net.minecraft.network.chat.Style;

public final class GlyphVisual {
    private Style style;
    private int color;
    private float alpha = 1.0F;
    private float offsetX;
    private float offsetY;
    private boolean visible = true;

    public GlyphVisual(Style style) {
        this.style = style;
        this.color = style.getColor() == null ? 0xFFFFFF : style.getColor().getValue();
    }

    public GlyphVisual(GlyphVisual source) {
        this.style = source.style;
        this.color = source.color;
        this.alpha = source.alpha;
        this.offsetX = source.offsetX;
        this.offsetY = source.offsetY;
        this.visible = source.visible;
    }

    public Style style() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    public float alpha() {
        return alpha;
    }

    public void multiplyAlpha(float factor) {
        this.alpha = Math.max(0.0F, Math.min(1.0F, this.alpha * factor));
    }

    public float offsetX() {
        return offsetX;
    }

    public void addOffsetX(float offsetX) {
        this.offsetX += offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    public void addOffsetY(float offsetY) {
        this.offsetY += offsetY;
    }

    public boolean visible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int argb() {
        return ((int) (alpha * 255.0F) << 24) | color;
    }
}

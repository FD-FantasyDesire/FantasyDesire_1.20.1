#version 150

in vec4 vertexColor;
in vec2 localCoord;
flat in int starShape;
out vec4 fragColor;

// 局部距离场的高斯亮核；远处小于像素的核心保持能量，避免闪烁和变粗。
float softCore(float distance, float width, float pixel) {
    float filtered = sqrt(width * width + pixel * pixel);
    return exp(-distance * distance / (filtered * filtered)) * width / filtered;
}

float starRay(vec2 p, vec2 pixel) {
    float along = abs(p.x);
    float width = 0.009 + 0.085 * exp(-along * 12.0);
    return softCore(abs(p.y), width, pixel.y) * exp(-along * 3.8)
        * (1.0 - smoothstep(0.55, 0.92, along));
}

void main() {
    vec2 p = localCoord;
    // 导数在一致控制流中计算，两个图元分支均使用已有结果。
    vec2 pixel = max(fwidth(p) * 0.65, vec2(0.0001));
    vec3 tint = vertexColor.rgb;
    float energy = max(tint.r, max(tint.g, tint.b));
    vec3 whiteCore = mix(tint, vec3(energy), 0.82);
    vec3 emission;
    if (starShape == 0) {
        float distance = abs(p.y);
        float core = softCore(distance, 0.065, pixel.y);
        float sheath = softCore(distance, 0.22, pixel.y);
        float halo = exp(-distance * 5.5) * (1.0 - smoothstep(0.65, 1.0, distance));
        emission = whiteCore * core * 1.05 + tint * (sheath * 0.48 + halo * 0.14);
    } else {
        float radius = length(p);
        float cross = starRay(p / vec2(0.84, 1.0), pixel / vec2(0.84, 1.0))
            + starRay(p.yx / vec2(1.0, 0.84), pixel.yx / vec2(1.0, 0.84));
        vec2 diagonal = vec2(p.x + p.y, p.y - p.x) * 1.35;
        vec2 diagonalPixel = vec2(pixel.x + pixel.y) * 1.35;
        float glints = (starRay(diagonal, diagonalPixel) + starRay(diagonal.yx, diagonalPixel)) * 0.10;
        float nucleus = softCore(radius, 0.045, length(pixel));
        float halo = exp(-radius * 8.0) * (1.0 - smoothstep(0.60, 0.96, radius));
        emission = whiteCore * (cross * 1.05 + nucleus * 0.65 + glints)
            + tint * (halo * 0.65 + cross * 0.18);
    }
    // 加法发光已乘生命周期透明度；没有背景、全屏色调映射或隐含 Bloom。
    fragColor = vec4(emission * vertexColor.a, 0.0);
}

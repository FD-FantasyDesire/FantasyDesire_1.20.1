#version 150

in vec4 vertexColor;
in vec2 localCoord;
flat in int shape;
flat in float trailJoinFraction;
out vec4 fragColor;

float softCore(float distance, float width, float pixel) {
    float filtered = sqrt(width * width + pixel * pixel);
    return exp(-distance * distance / (filtered * filtered)) * width / filtered;
}

void main() {
    vec2 p = localCoord;
    vec2 pixel = max(fwidth(p) * 0.65, vec2(0.0001));
    vec3 tint = vertexColor.rgb;
    float energy = max(tint.r, max(tint.g, tint.b));
    vec3 whiteCore = mix(tint, vec3(energy), 0.82);
    vec3 emission;
    if (shape == 0) {
        float across = abs(p.y);
        float tailWidth = mix(1.0, 0.025, clamp(p.x, 0.0, 1.0));
        float core = softCore(across, 0.14 * tailWidth, pixel.y);
        float sheath = softCore(across, 0.38 * tailWidth, pixel.y);
        float edge = 1.0 - smoothstep(0.65, 1.0, across);
        float taper = pow(max(0.0, 1.0 - p.x), 1.6);
        // 根部亮度与一阶变化均归零，消除星核中心的硬截面，宽辉光也使用同一过渡。
        float join = smoothstep(0.0, max(trailJoinFraction, 0.000001), p.x);
        emission = (whiteCore * core * 0.7 + tint * sheath * 0.4) * edge * taper * join;
    } else if (shape == 2) {
        float radius = length(p);
        float nucleus = softCore(radius, 0.045, length(pixel));
        float halo = exp(-radius * 8.0) * (1.0 - smoothstep(0.60, 0.96, radius));
        emission = whiteCore * nucleus * 0.65 + tint * halo * 0.65;
    } else {
        float along = abs(p.x);
        // AstraLightning 的收尖轮廓，横向按独立 width 重标定；方片半宽为 4 * width。
        float width = (0.009 + 0.085 * exp(-along * 12.0)) / (0.094 * 4.0);
        float ray = softCore(abs(p.y), width, pixel.y) * exp(-along * 3.8)
                * (1.0 - smoothstep(0.55, 0.92, along))
                * (1.0 - smoothstep(0.75, 1.0, abs(p.y)));
        emission = shape == 3 ? whiteCore * ray * 0.10 : (whiteCore * 1.05 + tint * 0.18) * ray;
    }
    // 加法发光，alpha 同时控制星芒和拖尾；所有时间与生命周期计算在 Java 中按 tick 完成。
    fragColor = vec4(emission * vertexColor.a, 0.0);
}

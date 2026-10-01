#version 150

uniform mat4 InverseViewProjection;
uniform vec3 CameraPosition;
uniform vec3 HoleCenter;
uniform vec2 ScreenSize;
uniform float EffectTime;
uniform float Exposure;
uniform float DiscBrightness;
uniform float LensingStrength;
uniform int Steps;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265359;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash13(i), hash13(i + vec3(1,0,0)), f.x),
                   mix(hash13(i + vec3(0,1,0)), hash13(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash13(i + vec3(0,0,1)), hash13(i + vec3(1,0,1)), f.x),
                   mix(hash13(i + vec3(0,1,1)), hash13(i + vec3(1,1,1)), f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float n = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; ++i) {
        n += a * noise3(p);
        p = p * 2.03 + vec3(7.1, 3.2, 5.7);
        a *= 0.48;
    }
    return n;
}

vec3 starLayer(vec3 d, float scale, float pixelAngle) {
    vec3 q = d * scale;
    vec3 cell = floor(q);
    vec3 light = vec3(0);
    // 三维邻域避免球面经纬接缝；只保留接近单位球面的星点。
    for (int z = -1; z <= 1; ++z) {
        for (int y = -1; y <= 1; ++y) {
            for (int x = -1; x <= 1; ++x) {
                vec3 id = cell + vec3(x,y,z);
                float seed = hash13(id + 19.7);
                if (seed > 0.986) {
                    vec3 pt = id + vec3(hash13(id), hash13(id + 31.3), hash13(id + 71.9));
                    float shell = abs(length(pt) - scale);
                    float dist = length(cross(d, pt));
                    float size = max(0.035, pixelAngle * scale * 0.48);
                    float core = exp(-dist * dist / (size * size));
                    float halo = exp(-dist * dist / (size * size * 12.0)) * 0.06;
                    float brightness = 0.35 + 2.0 * pow(hash13(id + 91.2), 8.0);
                    vec3 tint = mix(vec3(0.48,0.73,1.0), vec3(1.0,0.8,0.5), hash13(id + 44.1));
                    light += tint * (core + halo) * brightness * (1.0 - smoothstep(0.35,0.9,shell));
                }
            }
        }
    }
    return light;
}

vec3 universe(vec3 d, float pixelAngle) {
    float band = exp(-pow(dot(d, normalize(vec3(0.4,0.82,0.38))) + 0.12, 2.0) * 18.0);
    float gas = fbm(d * 6.0 + vec3(4.3,9.1,2.7));
    float detail = fbm(d * 19.0 + vec3(3,1,7));
    float dust = smoothstep(0.35,0.7,fbm(d * 11.0 + 19.0));
    vec3 clouds = mix(vec3(0.025,0.15,0.19), vec3(0.26,0.075,0.12),
                      smoothstep(0.35,0.65,noise3(d * 4.0 + 7.0)));
    vec3 color = vec3(0.0015,0.003,0.007);
    color += clouds * band * pow(gas, 2.0) * (0.3 + detail) * (1.0 - dust * 0.83);
    color += vec3(0.10,0.15,0.17) * pow(detail,5.0) * band;
    color += starLayer(d, 90.0, pixelAngle);
    color += starLayer(d, 160.0, pixelAngle) * 0.7;
    return color;
}

vec3 discEmission(vec3 p, vec3 v) {
    float r = max(length(p.xz), 0.001);
    // EffectTime 为秒；差动角速度按半径的 -3/2 次方变化。
    float phase = atan(p.z,p.x) - EffectTime * 1.7 / pow(r,1.5);
    vec3 flow = vec3(cos(phase) * r, sin(phase) * r, r * 1.4);
    float turbulence = fbm(flow * 3.5);
    float filaments = sin(r * 32.0 + turbulence * 7.0 + sin(phase * 5.0 + r * 3.0) * 1.4);
    float fine = noise3(flow * 17.0);
    float density = (0.33 + turbulence * 0.9) * (0.65 + 0.35 * filaments) * (0.8 + fine * 0.4);
    vec3 tangent = vec3(-p.z,0,p.x) / r;
    float beta = sqrt(0.5 / max(r - 1.0, 0.1));
    float doppler = sqrt(max(1.0 - beta * beta,0.01)) /
                    max(1.0 + beta * dot(normalize(v),tangent),0.2);
    float shift = doppler * sqrt(max(1.0 - 1.0 / r,0.01));
    float heat = pow(3.0 / r,0.7) * shift;
    vec3 hot = mix(vec3(1.0,0.12,0.018),vec3(1.0,0.66,0.22),smoothstep(0.35,0.75,heat));
    hot = mix(hot,vec3(0.76,0.88,1.0),smoothstep(0.85,1.45,heat));
    return hot * density * pow(3.1 / r,2.0) * pow(clamp(shift,0.3,2.0),3.0) * max(DiscBrightness,0.0);
}

vec3 acceleration(vec3 p, float angularMomentum2) {
    float r2 = max(dot(p,p),0.25);
    // rs=1：非旋转黑洞零测地线的等效中心力，保留固定角动量。
    return -1.5 * clamp(LensingStrength,0.0,1.5) * angularMomentum2 * p / (r2 * r2 * sqrt(r2));
}

vec3 toneMap(vec3 c) {
    c *= max(Exposure,0.0);
    return clamp((c * (2.51 * c + 0.03)) / (c * (2.43 * c + 0.59) + 0.14),0.0,1.0);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(ScreenSize,vec2(1));
    vec4 farPoint = InverseViewProjection * vec4(uv * 2.0 - 1.0,1.0,1.0);
    vec3 world = farPoint.xyz / max(abs(farPoint.w),1e-6) * sign(farPoint.w);
    vec3 p = CameraPosition - HoleCenter;
    vec3 v = normalize(world - CameraPosition);
    float angularMomentum2 = dot(cross(p,v),cross(p,v));
    vec3 color = vec3(0);
    float transmission = 1.0;
    bool escaped = false;
    int budget = clamp(Steps,64,420);

    // 有界速度 Verlet 积分；盘面附近缩小步长，避免穿过薄盘时漏采样。
    for (int i = 0; i < 420; ++i) {
        if (i >= budget) break;
        float r = length(p);
        if (r < 1.015) { transmission = 0.0; break; }
        if (r > 45.0 && dot(p,v) > 0.0) { escaped = true; break; }
        float stepSize = clamp(r * 0.11,0.045,1.4);
        if (r < 8.0 && abs(p.y) < 0.65) stepSize = min(stepSize,0.10);
        vec3 a = acceleration(p,angularMomentum2);
        vec3 next = p + v * stepSize + 0.5 * a * stepSize * stepSize;
        vec3 nv = v + 0.5 * (a + acceleration(next,angularMomentum2)) * stepSize;
        // 穿越 y=0 时在真实交点着色；其余采样使用线段中点。
        float dy = next.y - p.y;
        float fraction = (p.y * next.y < 0.0 && abs(dy) > 1e-6) ? clamp(-p.y / dy,0.0,1.0) : 0.5;
        vec3 sampleP = mix(p,next,fraction);
        float discR = length(sampleP.xz);
        if (discR > 2.85 && discR < 7.0 && abs(sampleP.y) < 0.65) {
            float radial = smoothstep(2.85,3.25,discR) * (1.0 - smoothstep(5.4,7.0,discR));
            float thickness = 0.055 + discR * 0.012;
            float column;
            if (abs(dy) > 0.001) {
                float c0 = tanh(p.y / thickness * 1.15);
                float c1 = tanh(next.y / thickness * 1.15);
                column = abs(c1 - c0) * thickness / (2.3 * abs(dy));
            } else {
                column = exp(-pow(sampleP.y / thickness,2.0));
            }
            float alpha = 1.0 - exp(-column * radial * stepSize * 4.5);
            vec3 emitted = discEmission(sampleP,nv);
            color += transmission * emitted * alpha;
            // 宽层散射是局部解析辉光，不依赖宿主 Bloom。
            float haze = exp(-abs(sampleP.y) / 0.23) * radial * stepSize * 0.065;
            color += transmission * emitted * haze;
            transmission *= 1.0 - alpha * 0.88;
        }
        p = next;
        v = nv;
        if (transmission < 0.003) break;
    }

    // 微分必须位于一致控制流，不能在提前退出的追踪循环里计算。
    vec3 outDirection = normalize(v);
    float footprint = max(length(dFdx(outDirection)),length(dFdy(outDirection)));
    footprint = clamp(footprint,0.0002,0.008);
    if (escaped) color += transmission * universe(outDirection,footprint);
    vec3 mapped = pow(toneMap(color),vec3(1.0 / 2.2));
    fragColor = vec4(mapped,1.0);
}

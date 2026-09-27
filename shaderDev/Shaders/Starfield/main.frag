// 程序化星空：GLSL Canvas / WebGL 1 / GLSL ES 1.00，零纹理。
// 星图在二维连续坐标中固定，时间只驱动观察漂移、闪烁和偶发流星。
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform vec2 u_resolution;  // 实际画布尺寸，像素
uniform float u_time;       // 从预览开始累计的秒数
uniform vec2 u_mouse;       // 左下角为原点的像素坐标；(0, 0) 表示中性视角

// STARFIELD_SHARED_BEGIN：StarSea 宿主复用此段，保持本文件可独立预览。
const float SEED = 27.0;
const float STAR_DENSITY = 1.0;   // 建议 0.0–2.0
const float NEBULA_GAIN = 1.0;    // 建议 0.0–2.0；0 为纯星点背景
const float TWINKLE = 0.20;       // 建议 0.0–1.0
const float DRIFT = 0.035;        // 180 秒闭合漂移轨迹的坐标振幅；0 为固定镜头
const float METEOR_GAIN = 0.65;   // 0 关闭流星
const float EXPOSURE = 1.15;
#define CLOUD_OCTAVES 5          // 建议 3–5；编译期固定上限

const float TAU = 6.28318530718;

vec3 hash23(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    q += dot(q, q.yxz + 33.33);
    return fract((q.xxy + q.yzz) * q.zyx);
}

float noise2(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash23(cell).x, hash23(cell + vec2(1.0, 0.0)).x, u.x),
               mix(hash23(cell + vec2(0.0, 1.0)).x,
                   hash23(cell + vec2(1.0)).x, u.x), u.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float weight = 0.5;
    float total = 0.0;
    for (int i = 0; i < CLOUD_OCTAVES; i++) {
        sum += weight * noise2(p);
        total += weight;
        p = mat2(0.80, 0.60, -0.60, 0.80) * p * 2.03 + vec2(13.7, 7.9);
        weight *= 0.5;
    }
    return sum / max(total, 0.001);
}

// 返回银河辐射；透过率只衰减远处星层，让前景恒星穿过暗尘埃。
vec3 galaxy(vec2 p, out float transmission) {
    vec2 q = mat2(0.862, -0.507, 0.507, 0.862) * p;
    vec2 seedOffset = vec2(SEED * 1.73, SEED * 0.91);
    vec2 warp = vec2(fbm(q * 1.6 + seedOffset),
                     fbm(q * 1.6 + seedOffset + vec2(18.2, 5.7))) - 0.5;
    vec2 cloudCoord = q * vec2(1.25, 2.3) + warp * 1.5 + seedOffset;
    float clouds = fbm(cloudCoord * 3.0);
    float filaments = fbm(cloudCoord * 8.0 + vec2(8.4, 3.1));
    float spine = q.y + warp.y * 0.42 + sin(q.x * 0.8) * 0.07;
    float band = exp(-spine * spine / 0.105);
    float outer = exp(-spine * spine / 0.48);
    float structure = pow(clamp(clouds * 1.3, 0.0, 1.0), 2.4);
    float dustAxis = spine + 0.045 + (filaments - 0.5) * 0.19;
    float dust = exp(-dustAxis * dustAxis / 0.007)
        * smoothstep(0.30, 0.67, clouds + filaments * 0.18);
    transmission = 1.0 - dust * 0.78 * clamp(NEBULA_GAIN, 0.0, 1.0);
    float colorMix = smoothstep(0.25, 0.75, warp.x + 0.5 + q.x * 0.09);
    vec3 gas = mix(vec3(0.030, 0.060, 0.125), vec3(0.110, 0.043, 0.105), colorMix);
    vec3 emission = gas * band * structure * (0.4 + filaments * 1.5);
    emission += vec3(0.022, 0.028, 0.048) * outer * clouds * 0.35;
    emission += vec3(0.12, 0.095, 0.075) * band * pow(clouds, 5.0) * 0.9;
    return emission * (1.0 - dust * 0.9) * max(NEBULA_GAIN, 0.0);
}

vec3 starColor(float temperature) {
    vec3 warm = vec3(1.0, 0.70, 0.43);
    vec3 white = vec3(0.88, 0.93, 1.0);
    vec3 blue = vec3(0.49, 0.69, 1.0);
    return temperature < 0.25 ? mix(warm, white, temperature * 4.0)
        : mix(white, blue, (temperature - 0.25) / 0.75);
}

// 三层各检查 3×3 相邻网格，星核和光晕不会在单元边界被切断。
// 支撑半径不超过 0.46 格；低分辨率时也保持该上限。
vec3 stars(vec2 p, float scale, float occupancy, float radius,
           float brightness, float salt, float glints, float pixel, float seconds) {
    vec2 grid = p * scale;
    vec2 cell = floor(grid);
    vec2 local = fract(grid);
    float aa = min(pixel * scale * 0.55, 0.10);
    vec3 sum = vec3(0.0);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbour = vec2(float(x), float(y));
            vec3 random = hash23(cell + neighbour + vec2(salt, SEED * 3.17));
            if (random.z < clamp(occupancy * STAR_DENSITY, 0.0, 1.0)) {
                vec3 detail = hash23(cell + neighbour + vec2(SEED, salt + 42.6));
                vec2 d = local - neighbour - (0.12 + random.xy * 0.76);
                float distance2 = dot(d, d);
                float r = radius * mix(0.65, 1.35, detail.x);
                float filtered2 = r * r + aa * aa;
                // 展宽星核时守恒二维高斯积分，避免低分辨率下所有星点变亮。
                float core = exp(-distance2 / filtered2) * r * r / filtered2;
                float halo = exp(-sqrt(distance2) / max(r * 3.8, 0.001)) * mix(0.008, 0.055, glints);
                float rays = 0.0;
                if (glints > 0.0 && detail.x > 0.72) {
                    vec2 a = abs(d);
                    float rayWidth2 = r * r * 0.16 + aa * aa;
                    float rayNorm = r * 0.4 / sqrt(rayWidth2);
                    rays = (exp(-a.x * a.x / rayWidth2 - a.y / 0.115)
                          + exp(-a.y * a.y / rayWidth2 - a.x / 0.115)) * rayNorm * 0.23;
                }
                float frequency = mix(0.55, 1.45, detail.y); // 弧度/秒；各星独立相位
                float wave = sin(seconds * frequency + detail.z * TAU)
                    * sin(seconds * frequency * 0.37 + random.x * TAU);
                float shimmer = 1.0 + clamp(TWINKLE, 0.0, 1.0) * wave;
                float support = 1.0 - smoothstep(0.30, 0.46, sqrt(distance2));
                float magnitude = mix(0.35, 1.0, detail.x * detail.x);
                sum += starColor(detail.z) * (core + halo + rays * glints)
                    * support * magnitude * brightness * shimmer;
            }
        }
    }
    return sum;
}

// STARFIELD_SHARED_END

// 每 17 秒出现一次有限尾迹；换种子前后均已完全淡出，不留下跨周期线段。
vec3 meteor(vec2 p, float seconds, float pixel) {
    float cycle = floor(seconds / 17.0);
    float age = mod(seconds, 17.0) - 2.0;
    if (age <= 0.0 || age >= 1.4 || METEOR_GAIN <= 0.0) return vec3(0.0);
    vec3 random = hash23(vec2(cycle + SEED, 91.7));
    vec2 start = vec2(mix(-0.85, 0.65, random.x), mix(0.35, 0.95, random.y));
    vec2 direction = normalize(vec2(1.0, -0.48 - random.z * 0.35));
    vec2 head = start + direction * age * 0.85;
    vec2 delta = p - head;
    float along = dot(delta, direction);
    float across = abs(dot(delta, vec2(-direction.y, direction.x)));
    float trailLength = 0.32 * smoothstep(0.0, 0.18, age);
    float width = max(pixel * 0.65, 0.0011);
    float trail = exp(-across * across / (width * width))
        * exp(min(along, 0.0) / 0.11)
        * smoothstep(-trailLength - width, -trailLength + width, along)
        * (1.0 - smoothstep(-width, width, along));
    float core = exp(-dot(delta, delta) / (width * width * 2.0));
    float envelope = smoothstep(0.0, 0.12, age) * (1.0 - smoothstep(0.75, 1.4, age));
    return vec3(0.61, 0.78, 1.0) * (trail * 0.65 + core) * envelope * METEOR_GAIN;
}

void main() {
    vec2 resolution = max(u_resolution, vec2(1.0));
    float shortSide = min(resolution.x, resolution.y);
    float pixel = 2.0 / shortSide;
    vec2 screen = (2.0 * gl_FragCoord.xy - resolution) / shortSide;
    float seconds = max(u_time, 0.0);
    vec2 mouse = vec2(0.0);
    if (max(u_mouse.x, u_mouse.y) > 0.0) {
        mouse = clamp(u_mouse / resolution * 2.0 - 1.0, -1.0, 1.0);
    }
    float orbit = mod(seconds, 180.0) * (TAU / 180.0);
    vec2 p = screen + mouse * vec2(0.55, 0.35)
        + DRIFT * vec2(sin(orbit), cos(orbit) - 1.0);
    float transmission;
    vec3 cloud = galaxy(p, transmission);
    vec3 color = vec3(0.0016, 0.0026, 0.0065) + cloud;
    color += stars(p, 58.0, 0.085, 0.036, 1.1, 17.0, 0.0, pixel, seconds) * transmission;
    color += stars(p, 29.0, 0.12, 0.027, 2.0, 53.0, 0.0, pixel, seconds) * mix(1.0, transmission, 0.35);
    color += stars(p, 12.5, 0.10, 0.024, 3.6, 97.0, 1.0, pixel, seconds);
    color += meteor(p, seconds, pixel);
    // 预览输出为不透明背景；曝光映射与近似 sRGB 编码在最后执行。
    color = vec3(1.0) - exp(-max(color, vec3(0.0)) * max(EXPOSURE, 0.0));
    gl_FragColor = vec4(pow(color, vec3(1.0 / 2.2)), 1.0);
}

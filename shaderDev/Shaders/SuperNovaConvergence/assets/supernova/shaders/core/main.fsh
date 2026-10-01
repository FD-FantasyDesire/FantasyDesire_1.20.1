#version 150

uniform sampler2D DepthSampler;
uniform sampler2D AuraSampler;
uniform vec2 RenderSize;
// -1：原分辨率直接绘制；0：仅积分云团；1：重建云团并绘制原分辨率细节。
uniform int ResolvePass;
uniform float CloudOpacity;
uniform mat4 InverseViewProjection;
uniform mat3 IViewRotMat;
uniform vec3 CameraPosition;
uniform vec2 ScreenSize;
uniform vec3 EffectCenter;
uniform float EffectScale;
// EffectAge、ExplosionTime、AgeStagger 均为秒；生命周期不使用归一化 GameTime。
uniform float EffectAge;
uniform float ExplosionTime;
uniform float Seed;
uniform float Exposure;
uniform int VolumeSteps;
uniform int TrailSteps;
uniform int InstanceCount;
uniform int InstanceColumns;
uniform float InstanceSpacing;
uniform float SeedStride;
uniform float AgeStagger;
uniform int RepeatPreview;
uniform vec4 ColorModulator;

flat in mat4 viewProjection;
out vec4 fragColor;

const float AFTERMATH_DURATION = 3.6;
const int PAIR_COUNT = 7;
const int PARTICLES = PAIR_COUNT * 2;
const int MAX_TRAIL_STEPS = 18;
const int MAX_VOLUME_STEPS = 80;
const int MAX_INSTANCES = 32;
const float PI = 3.14159265359;
const float TAU = 6.28318530718;
const vec3 IVORY = vec3(1.0, 0.92, 0.75);
const vec3 CYAN = vec3(0.12, 0.72, 1.0);

// 每个实例的局部坐标以 EffectScale 格为单位；深度上限也转换到同一空间。
vec3 instanceCenter;
float localScale;
float sceneLimit;
vec2 screenOrigin;
vec2 aspectScale;
mat3 seedMatrix;
vec3 noiseOffset;

float visiblePoint(vec3 p, vec3 ro, vec3 forward) {
    float along = dot(p - ro, forward);
    float rayDistance = length(p - ro);
    return step(0.05 / localScale, along) * step(rayDistance, sceneLimit + 0.002);
}

float sat(float x) { return clamp(x, 0.0, 1.0); }
float sq(float x) { return x * x; }
float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    return fract(p * (p + p));
}
float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
mat2 rotate2(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, s, -s, c);
}
float noise3(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash31(i), hash31(i + vec3(1,0,0)), f.x),
                   mix(hash31(i + vec3(0,1,0)), hash31(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash31(i + vec3(0,0,1)), hash31(i + vec3(1,0,1)), f.x),
                   mix(hash31(i + vec3(0,1,1)), hash31(i + vec3(1,1,1)), f.x), f.y), f.z);
}
float fbm(vec3 p) {
    float f = 0.0, a = 0.55;
    for (int i = 0; i < 4; ++i) {
        f += a * noise3(p);
        p = p.yzx * 2.03 + vec3(9.2, 3.8, 7.1);
        a *= 0.48;
    }
    return f;
}
vec3 seedRotation(vec3 p, float seed) {
    p.xz = rotate2(TAU * hash11(seed + 3.1)) * p.xz;
    p.yz = rotate2((hash11(seed + 7.8) - 0.5) * 1.3) * p.yz;
    p.xy = rotate2((hash11(seed + 1.9) - 0.5) * 0.8) * p.xy;
    return p;
}
vec3 streamColor(float i) {
    if (i < 0.5) return vec3(0.12, 1.0, 0.82);
    if (i < 1.5) return vec3(1.0, 0.51, 0.10);
    if (i < 2.5) return vec3(0.85, 0.24, 0.62);
    if (i < 3.5) return vec3(0.55, 1.0, 0.18);
    if (i < 4.5) return vec3(0.17, 0.58, 1.0);
    if (i < 5.5) return vec3(1.0, 0.25, 0.09);
    return vec3(0.77, 0.68, 1.0);
}

// x=出生时剩余秒数，y=抵达时剩余秒数。每对共享计划，最后一对恰好用尽倒计时。
vec2 pairSchedule(float pair, float seed, float explosionTime) {
    float slot = pair / float(PAIR_COUNT - 1);
    float birthRemaining = 0.94 - slot * 0.19 - hash11(seed + pair * 7.7) * 0.035;
    float arrivalRemaining = (1.0 - slot) * (0.40 + hash11(seed + pair * 2.3) * 0.03);
    return vec2(birthRemaining, arrivalRemaining) * explosionTime;
}
void particlePath(float i, float seed, out vec3 source,
                  out vec3 controlA, out vec3 controlB, out vec3 target) {
    float pair = floor(i * 0.5);
    float side = mod(i, 2.0) < 0.5 ? 1.0 : -1.0;
    float h = hash11(seed + pair * 11.31);
    float angle = pair / float(PAIR_COUNT) * PI + (h - 0.5) * 0.28;
    // 每对的另一条轨迹由整个控制多边形关于爆心反射得到，随机扰动也保持成对。
    vec3 radial = normalize(vec3(cos(angle), sin(angle), (hash11(h * 43.0) - 0.5) * 1.6));
    vec3 tangent = normalize(cross(radial, vec3(0.18, -0.12, 1.0)));
    float radius = 2.65 + hash11(seed + pair * 8.9) * 0.65;
    target = vec3(0.0);
    source = target + side * seedRotation(radial * radius, seed);
    controlA = target + side * seedRotation(radial * radius * 0.93 + tangent * (1.15 + h * 0.5), seed);
    controlB = target + side * seedRotation(radial * radius * 0.12 + tangent * (0.54 + h * 0.52), seed);
}
vec3 bezier(vec3 a, vec3 b, vec3 c, vec3 d, float t) {
    float v = 1.0 - t;
    return v*v*v*a + 3.0*v*v*t*b + 3.0*v*t*t*c + t*t*t*d;
}
vec2 projectPoint(vec3 p, vec3 ro, vec3 right, vec3 up, vec3 forward) {
    vec4 clip = viewProjection * vec4(instanceCenter + p * localScale, 1.0);
    return clip.xy / max(clip.w, 0.001) * aspectScale - screenOrigin;
}
float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 ab = b - a;
    float h = sat(dot(p - a, ab) / max(dot(ab, ab), 0.0000001));
    return length(p - a - ab * h);
}
float star(vec2 p, float size, float angle) {
    p = rotate2(angle) * p;
    // 菱形臂有有限端点，外层辉光也在同一包围域内归零。
    float x = abs(p.x), y = abs(p.y);
    float horizontal = x / size + y / (size * 0.065);
    float vertical = y / (size * 1.23) + x / (size * 0.065);
    // 距离场并集只计算一次覆盖率，交叉处不重复叠加颜色或透明度。
    float shape = 1.0 - smoothstep(0.72, 1.0, min(horizontal, vertical));
    float r = length(p);
    float halo = exp(-r * r / max(size * size * 0.1, 0.000001));
    float cutoff = 1.0 - smoothstep(size * 1.3, size * 1.8, r);
    return (shape + (1.0 - shape) * halo * 0.26) * cutoff;
}

vec3 convergence(vec2 uv, vec3 ro, vec3 right, vec3 up, vec3 forward,
                 float t, float seed, float ignition, float pixel) {
    vec3 light = vec3(0.0);
    float remaining = ignition - t;
    for (int j = 0; j < PARTICLES; ++j) {
        float i = float(j);
        float pair = floor(i * 0.5);
        vec2 schedule = pairSchedule(pair, seed, ignition);
        if (remaining <= schedule.x && remaining > schedule.y) {
            float age = (schedule.x - remaining) / max(schedule.x - schedule.y, 0.0001);
            float head = pow(sat(age), 1.65);
            // 只画当前头部往回一小段时间窗；没有累计全轨道或幽灵起点。
            float tail = pow(sat(age - 0.34), 1.65);
            tail = mix(tail, head, smoothstep(0.83, 1.0, age));
            vec3 a, b, c, d;
            particlePath(i, seed, a, b, c, d);
            vec3 hue = streamColor(pair);
            float life = smoothstep(0.0, 0.08, age);
            int trailCount = clamp(TrailSteps, 4, MAX_TRAIL_STEPS);
            for (int k = 0; k < MAX_TRAIL_STEPS; ++k) {
                if (k >= trailCount) break;
                float f0 = float(k) / float(trailCount);
                float f1 = float(k + 1) / float(trailCount);
                vec3 p0 = bezier(a, b, c, d, mix(tail, head, f0));
                vec3 p1 = bezier(a, b, c, d, mix(tail, head, f1));
                vec2 s0 = projectPoint(p0, ro, right, up, forward);
                vec2 s1 = projectPoint(p1, ro, right, up, forward);
                if (dot(p0 - ro, forward) <= 0.05 / localScale
                    || dot(p1 - ro, forward) <= 0.05 / localScale) continue;
                float dist = segmentDistance(uv, s0, s1);
                vec2 edge = s1 - s0;
                float nearest = sat(dot(uv - s0, edge) / max(dot(edge, edge), 0.0000001));
                float visible = visiblePoint(mix(p0, p1, nearest), ro, forward);
                float depthScale = 9.0 / max(length(ro - p1), 0.001);
                float width = max(pixel * 0.7, (0.0009 + 0.0017 * f1) * depthScale);
                float taper = smoothstep(0.0, 0.35, f1);
                float core = exp(-sq(dist / width) * 1.6);
                float glow = exp(-sq(dist / (width * 4.8))) * 0.1;
                // 分段取最大值式覆盖的近似权重，避免连接点累加成为珠串。
                float segmentLength = max(length(s1 - s0), pixel);
                float weight = min(1.0, segmentLength / (width * 2.8));
                light += (hue * (core + glow) + IVORY * core * 0.16) * taper * life * weight * visible;
            }
            vec3 point = bezier(a, b, c, d, head);
            life *= visiblePoint(point, ro, forward);
            vec2 screen = projectPoint(point, ro, right, up, forward);
            float depthScale = 9.0 / max(length(ro - point), 0.001);
            float radius = max(0.0062 * depthScale, pixel * 1.1);
            float dist = length(uv - screen);
            float body = exp(-sq(dist / radius) * 1.5);
            float glow = exp(-sq(dist / (radius * 4.0))) * 0.25;
            light += (mix(hue, IVORY, 0.67) * body * 3.2 + hue * glow) * life;
            light += hue * star(uv - screen, radius * 3.4, pair * 1.2) * life * 0.65;
        }
    }
    return light;
}

// 初始星核和爆发星芒是同一层，只改变尺寸、角度、亮度与生命周期。
// 返回预乘颜色及覆盖率，中心颜色不受双星臂或体积背景重复叠加影响。
vec4 centralStar(vec2 uv, float t, float seed, float explosionTime) {
    float age = t - explosionTime;
    if (age >= 0.45) return vec4(0.0);
    float charge = 0.0, rotation = 0.0, impact = 0.0;
    float response = explosionTime * 0.045;
    for (int j = 0; j < PAIR_COUNT; ++j) {
        float pair = float(j);
        float arrival = explosionTime - pairSchedule(pair, seed, explosionTime).y;
        // 每对接近并进入中心时推进一次平滑旋转，爆发继续使用同一个角度。
        float received = smoothstep(arrival - response, arrival + response * 0.4, t);
        charge += received;
        rotation += received * (0.35 + hash11(seed + pair * 3.1) * 0.13);
        impact += exp(-max(t - arrival, 0.0) / max(response, 0.001)) * step(arrival, t);
    }
    charge /= float(PAIR_COUNT);
    float angle = 0.13 + (hash11(seed + 41.0) - 0.5) * 0.55 + rotation;
    float blend = smoothstep(0.0, 0.045, age);
    float chargedSize = 0.050 + charge * 0.065 + impact * 0.008;
    float flashSize = 0.10 + 0.58 * (1.0 - exp(-max(age, 0.0) * 28.0));
    float size = mix(chargedSize, flashSize, blend);
    float life = smoothstep(0.0, explosionTime * 0.06, t);
    if (age > 0.0) life *= exp(-age * 9.0) * (1.0 - smoothstep(0.28, 0.45, age));
    float coverage = star(uv, size, angle) * life;
    float brightness = mix(1.6 + charge * 0.8 + impact * 0.15, 4.0, blend);
    return vec4(IVORY * coverage * brightness, coverage);
}

vec2 raySphere(vec3 ro, vec3 rd, float r) {
    float b = dot(ro, rd), h = b * b - dot(ro, ro) + r * r;
    if (h < 0.0) return vec2(-1.0);
    h = sqrt(h);
    return vec2(-b - h, -b + h);
}

// 径向噪声控制云团轮廓；连续三维噪声控制内部卷曲和破碎。
// 所有噪声绑定爆炸的局部空间并随半径膨胀，不绑定屏幕或相机方向。
vec4 cloudSample(vec3 local, vec3 viewRay, float radius, float age, float seed) {
    vec3 p = seedMatrix * local;
    p /= vec3(1.0 + 0.07 * sin(seed), 0.96, 1.04);
    vec3 q = p / max(radius, 0.001);
    float r = length(q);
    vec3 n = q / max(r, 0.001);
    vec3 offset = noiseOffset;
    float cavity = mix(0.12, 0.65, smoothstep(0.12, 1.65, age));
    if (r > 1.15 || r < max(cavity - 0.27, 0.0)) return vec4(0.0);
    float lobes = noise3(n * 4.3 + offset);
float boundary = 0.80 + lobes * 0.28;
    float outside = 1.0 - smoothstep(boundary - 0.085, boundary + 0.015, r);
    if (outside <= 0.0) return vec4(0.0);
    vec3 curl = vec3(noise3(q * 3.1 + offset), noise3(q.yzx * 3.1 + offset + 7.0),
                     noise3(q.zxy * 3.1 + offset + 13.0)) - 0.5;
    vec3 flow = q * 5.5 + curl * 1.65 + offset - n * age * 0.5;
    float cloud = fbm(flow);
    float inside = smoothstep(cavity - 0.15, cavity + 0.12, r + (cloud - 0.5) * 0.24);
    float breakup = smoothstep(1.15, 2.85, age);
    float cloudMask = smoothstep(0.29 + breakup * 0.32, 0.65 + breakup * 0.16, cloud);
    float density = outside * inside * cloudMask;
    density *= 1.0 - smoothstep(2.0, 3.1, age);
    if (density <= 0.0) return vec4(0.0);
    float detail = noise3(flow * 2.2 + curl);

    // 亮的卷边与暗的紫色体积同时存在，避免整团曝光为白球。
    vec3 limbNormal = local + vec3(0.0001);
    limbNormal /= max(length(limbNormal), 0.0001);
    float limb = pow(1.0 - abs(dot(limbNormal, viewRay)), 2.0);
    float hotEdge = exp(-sq((r - boundary + 0.055) / 0.042)) * (0.14 + limb * 1.5);
    float filaments = pow(sat(1.0 - abs(cloud - 0.54) * 13.0), 2.0)
                    * smoothstep(0.25, 0.8, detail);
    float heat = exp(-age * 8.0);
    vec3 hue = mix(vec3(0.045, 0.012, 0.15), vec3(0.22, 0.045, 0.36), cloud);
    hue = mix(hue, vec3(0.015, 0.10, 0.25), smoothstep(0.40, 0.76, noise3(q * 2.7 + offset + 41.0)) * 0.76);
    float lighting = 0.28 + 0.72 * sat(dot(n, normalize(vec3(-0.6, 0.8, 0.5))) * 0.5 + 0.5);
    vec3 emission = hue * (0.24 + detail * 0.72) * lighting;
    emission += mix(vec3(0.35, 0.16, 0.88), vec3(0.12, 0.68, 1.0), detail)
              * (hotEdge * (0.7 + heat * 2.1) + filaments * 0.40);
    emission += IVORY * heat * (hotEdge * 1.3 + filaments * 0.38);
    return vec4(emission, density);
}

// 前向合成使用实际步长的 Beer-Lambert 透射率，调步数不会改变总体亮度。
vec4 integrateCloud(vec3 ro, vec3 rd, float age, float seed) {
    if (age <= 0.015 || age >= 3.1) return vec4(0.0);
    float expansion = 1.0 - exp(-max(age - 0.015, 0.0) * 7.0);
    float radius = 0.12 + 2.24 * expansion + 0.17 * age;
    float bound = radius * 1.22;
    vec2 hit = raySphere(ro, rd, bound);
    if (hit.y <= 0.0) return vec4(0.0);
    float start = max(hit.x, 0.0), end = min(hit.y, sceneLimit);
    if (end <= start) return vec4(0.0);
    int stepCount = clamp(VolumeSteps, 16, MAX_VOLUME_STEPS);
    float stepSize = (end - start) / float(stepCount);
    // 保持原采样网格，只跳过确定为空的球形空腔；0.93 为形变最小轴长。
    float emptyRadius = max(mix(0.12, 0.65, smoothstep(0.12, 1.65, age)) - 0.27, 0.0) * radius * 0.93;
    vec2 emptyHit = raySphere(ro, rd, emptyRadius);
    // 亚像素抖动固定在画布上，只影响采样位置，不改变云形或随机种子。
    float jitter = hash31(vec3(gl_FragCoord.xy, 0.17));
    float transmission = 1.0;
    vec3 result = vec3(0.0);
    for (int k = 0; k < MAX_VOLUME_STEPS; ++k) {
        if (k >= stepCount) break;
        float distanceAlong = start + (float(k) + 0.42 + jitter * 0.16) * stepSize;
        if (emptyRadius > 0.0 && distanceAlong > emptyHit.x && distanceAlong < emptyHit.y) {
            k = max(k, int(ceil((emptyHit.y - start) / stepSize - 0.42 - jitter * 0.16)) - 1);
            continue;
        }
        vec3 p = ro + rd * distanceAlong;
        vec4 sampleValue = cloudSample(p, rd, radius, age, seed);
        float alpha = 1.0 - exp(-sampleValue.a * stepSize * 4.4);
        result += transmission * sampleValue.rgb * alpha;
        transmission *= 1.0 - alpha;
        if (transmission < 0.018) break;
    }
    return vec4(result, 1.0 - transmission);
}

// 冲击前沿是薄球壳。射线弦长产生掠射亮边，噪声使它短暂断裂。
vec3 shockFront(vec3 ro, vec3 rd, float age, float seed) {
    if (age <= 0.02 || age >= 0.72) return vec3(0.0);
    float radius = 0.2 + 2.82 * (1.0 - exp(-age * 9.0)) + age * 0.22;
    vec2 outer = raySphere(ro, rd, radius);
    if (outer.y < 0.0) return vec3(0.0);
    vec2 inner = raySphere(ro, rd, max(radius - 0.025, 0.01));
    float chord = max(min(outer.y, sceneLimit) - max(outer.x, 0.0), 0.0);
    if (inner.y > 0.0) chord -= max(min(inner.y, sceneLimit) - max(inner.x, 0.0), 0.0);
    vec3 surface = (ro + rd * outer.x) / radius;
    float breaks = smoothstep(0.28, 0.6, noise3((seedMatrix * surface) * 9.0 + seed * 0.3));
    float life = smoothstep(0.02, 0.075, age) * (1.0 - smoothstep(0.15, 0.72, age));
    // 去掉正面极短弦的低亮度累积，只留下球面的掠射冲击边缘。
    float rim = smoothstep(0.16, 0.58, max(chord, 0.0));
    return mix(IVORY, CYAN, sat(age * 2.0)) * rim * breaks * life * 0.8;
}

vec3 burstSparks(vec2 uv, vec3 ro, vec3 right, vec3 up, vec3 forward,
                 float age, float seed, float pixel) {
    if (age <= 0.035 || age >= 1.55) return vec3(0.0);
    vec3 light = vec3(0.0);
    float life = smoothstep(0.035, 0.075, age) * (1.0 - smoothstep(0.4, 1.55, age));
    for (int j = 0; j < 22; ++j) {
        float i = float(j), h = hash11(i * 17.2 + seed);
        float z = 1.0 - 2.0 * (i + 0.5) / 22.0;
        float phi = i * 2.39996 + h * 0.4;
        vec3 dir = seedRotation(vec3(cos(phi) * sqrt(1.0-z*z), z, sin(phi) * sqrt(1.0-z*z)), seed);
        float radius = 0.2 + (2.6 + h * 0.8) * (1.0 - exp(-age * 6.0)) + age * 0.16;
        // 头尾均在外壳附近；禁止从爆心画无穷射线。
        vec3 a = dir * max(0.0, radius - (0.1 + h * 0.24) * life);
        vec3 b = dir * radius;
        if (dot(a - ro, forward) <= 0.05 / localScale
            || dot(b - ro, forward) <= 0.05 / localScale) continue;
        vec2 s0 = projectPoint(a,ro,right,up,forward);
        vec2 s1 = projectPoint(b,ro,right,up,forward);
        vec2 edge = s1 - s0;
        float nearest = sat(dot(uv - s0, edge) / max(dot(edge, edge), 0.0000001));
        float visibility = visiblePoint(mix(a, b, nearest), ro, forward);
        float d = segmentDistance(uv, s0, s1);
        float width = max(pixel * 0.55, 0.0010);
        float facing = dot(dir, ro / max(length(ro), 0.0001));
        float depthFade = mix(0.22, 1.0, smoothstep(-0.4, 0.4, facing));
        light += mix(CYAN, IVORY, h) * exp(-sq(d / width)) * life * depthFade * 0.82 * visibility;
    }
    return light;
}


vec3 displayColor(vec3 radiance) {
    return pow(vec3(1.0) - exp(-max(radiance, vec3(0.0)) * max(Exposure, 0.0)),
               vec3(1.0 / 2.2));
}

// 世界深度转为射线距离，保证低分辨率和原分辨率使用同一份场景快照。
float depthDistance(vec2 ndc, vec3 rd) {
    float depth = textureLod(DepthSampler, ndc * 0.5 + 0.5, 0.0).r;
    if (depth >= 0.999999) return 100000.0;
    vec4 world = InverseViewProjection * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return max(dot(world.xyz / world.w - CameraPosition, rd), 0.0);
}

vec3 worldRay(vec2 ndc) {
    vec4 world = InverseViewProjection * vec4(ndc, 1.0, 1.0);
    return normalize(world.xyz / world.w - CameraPosition);
}

vec3 slotCenter(int slot, int columns, int rows) {
    vec2 cell = vec2(float(slot % columns) - float(columns - 1) * 0.5,
                     float(slot / columns) - float(rows - 1) * 0.5);
    return EffectCenter + vec3(cell.x, 0.0, cell.y) * InstanceSpacing;
}

void prepareInstance(int slot, int columns, int rows, out vec3 ro, out float seed) {
    instanceCenter = slotCenter(slot, columns, rows);
    ro = (CameraPosition - instanceCenter) / localScale;
    seed = clamp(floor(Seed + float(slot) * SeedStride + 0.5), 0.0, 9999.0);
    seedMatrix = mat3(seedRotation(vec3(1,0,0), seed),
                      seedRotation(vec3(0,1,0), seed),
                      seedRotation(vec3(0,0,1), seed));
    noiseOffset = vec3(hash11(seed + 3.0), hash11(seed + 7.0), hash11(seed + 11.0)) * 21.0;
}

float instanceAge(int slot, float ignition) {
    float t = EffectAge - float(slot) * AgeStagger;
    if (t < 0.0) return -1.0;
    if (RepeatPreview != 0) t = mod(t, ignition + AFTERMATH_DURATION);
    return t;
}

bool cloudRayHit(vec3 ro, vec3 rd, float age) {
    float radius = 0.12 + 2.24 * (1.0 - exp(-max(age - 0.015, 0.0) * 7.0)) + 0.17 * age;
    vec2 hit = raySphere(ro, rd, radius * 1.22);
    return min(hit.y, sceneLimit) > max(hit.x, 0.0);
}

bool hasCloudRay(vec3 rd, float ignition, int count, int columns, int rows) {
    if (CloudOpacity <= 0.0) return false;
    for (int slot = 0; slot < MAX_INSTANCES; ++slot) {
        if (slot >= count) break;
        float age = instanceAge(slot, ignition) - ignition;
        if (age <= 0.015 || age >= 3.1) continue;
        vec3 ro = (CameraPosition - slotCenter(slot, columns, rows)) / localScale;
        if (cloudRayHit(ro, rd, age)) return true;
    }
    return false;
}

vec4 cloudLayer(vec3 rd, float ignition, int count, int columns, int rows) {
    vec4 total = vec4(0.0);
    if (CloudOpacity <= 0.0) return total;
    for (int slot = 0; slot < MAX_INSTANCES; ++slot) {
        if (slot >= count) break;
        float age = instanceAge(slot, ignition) - ignition;
        if (age <= 0.015 || age >= 3.1) continue;
        vec3 ro = (CameraPosition - slotCenter(slot, columns, rows)) / localScale;
        // 先用包围域剔除，再计算实例旋转与噪声种子。
        if (!cloudRayHit(ro, rd, age)) continue;
        float seed;
        prepareInstance(slot, columns, rows, ro, seed);
        vec4 cloud = integrateCloud(ro, rd, age, seed);
        vec3 color = cloud.a > 0.00001 ? displayColor(cloud.rgb / cloud.a) * cloud.a : vec3(0.0);
        vec4 layer = vec4(color, cloud.a) * sat(CloudOpacity);
        total = total * (1.0 - layer.a) + layer;
    }
    return total;
}

// 深度不一致的轮廓像素回退到精确积分，平坦区域直接重建预乘颜色。
bool resolveCloud(vec2 uv, vec2 ndc, out vec4 color) {
    ivec2 size = textureSize(AuraSampler, 0);
    vec2 pixel = uv * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(pixel));
    vec2 fraction = fract(pixel);
    vec3 forward = normalize(IViewRotMat * vec3(0,0,-1));
    float currentZ = depthDistance(ndc, worldRay(ndc)) * dot(worldRay(ndc), forward);
    color = vec4(0.0);
    bool compatible = true;
    for (int y = 0; y < 2; ++y) for (int x = 0; x < 2; ++x) {
        ivec2 coord = clamp(base + ivec2(x,y), ivec2(0), size - 1);
        vec2 sampleUv = (vec2(coord) + 0.5) / vec2(size);
        vec2 sampleNdc = sampleUv * 2.0 - 1.0;
        vec3 sampleRay = worldRay(sampleNdc);
        float sampleZ = depthDistance(sampleNdc, sampleRay) * dot(sampleRay, forward);
        compatible = compatible && abs(sampleZ - currentZ) <= max(0.002, currentZ * 0.005);
        vec2 weight = mix(vec2(1.0) - fraction, fraction, vec2(x,y));
        color += texelFetch(AuraSampler, coord, 0) * weight.x * weight.y;
    }
    return compatible;
}

void main() {
    vec2 res = max(RenderSize, vec2(1.0));
    vec2 uvScreen = gl_FragCoord.xy / res;
    vec2 ndc = uvScreen * 2.0 - 1.0;
    // 低分辨率目标可向上取整，投影比例始终以最终输出为准。
    vec2 finalSize = max(ScreenSize, vec2(1.0));
    float shortSide = min(finalSize.x, finalSize.y);
    aspectScale = finalSize / shortSide;
    vec3 rd = worldRay(ndc);
    localScale = max(EffectScale, 0.001);
    sceneLimit = depthDistance(ndc, rd) / localScale;
    float ignition = clamp(ExplosionTime, 0.5, 12.0);
    int count = clamp(InstanceCount, 1, MAX_INSTANCES);
    int columns = clamp(InstanceColumns, 1, count);
    int rows = (count + columns - 1) / columns;
    vec4 total = vec4(0.0);
    bool resolved = false;
    if (ResolvePass == 1) {
        if (hasCloudRay(rd, ignition, count, columns, rows)) resolved = resolveCloud(uvScreen, ndc, total);
        else resolved = true;
    }
    if (!resolved) total = cloudLayer(rd, ignition, count, columns, rows);

    if (ResolvePass != 0) {
        vec3 right = normalize(IViewRotMat * vec3(1,0,0));
        vec3 up = normalize(IViewRotMat * vec3(0,1,0));
        vec3 forward = normalize(IViewRotMat * vec3(0,0,-1));
        float pixel = 2.0 / shortSide;
        for (int slot = 0; slot < MAX_INSTANCES; ++slot) {
            if (slot >= count) break;
            float t = instanceAge(slot, ignition);
            if (t < 0.0 || t >= ignition + 1.55) continue;
            vec3 ro;
            float seed;
            prepareInstance(slot, columns, rows, ro, seed);
            vec2 bound = raySphere(ro, rd, 4.2);
            if (bound.y <= 0.0 || max(bound.x, 0.0) >= sceneLimit) continue;
            vec4 centerClip = viewProjection * vec4(instanceCenter, 1.0);
            screenOrigin = centerClip.xy / max(centerClip.w, 0.001) * aspectScale;
            vec2 uv = ndc * aspectScale - screenOrigin;
            float age = t - ignition;
            vec3 light = shockFront(ro, rd, age, seed)
                       + convergence(uv, ro, right, up, forward, t, seed, ignition, pixel)
                       + burstSparks(uv, ro, right, up, forward, age, seed, pixel);
            float starScale = abs((viewProjection * vec4(up, 0.0)).y) * aspectScale.y * 9.2
                            / (1.9 * max(length(ro), 0.01));
            vec4 core = centralStar(uv / max(starScale, 0.0001), t, seed, ignition);
            core *= visiblePoint(vec3(0.0), ro, forward);
            vec3 coreColor = core.a > 0.00001 ? displayColor(core.rgb / core.a) * core.a : vec3(0.0);
            total.rgb = total.rgb * (1.0 - core.a) + coreColor + displayColor(light);
            total.a = total.a * (1.0 - core.a) + core.a;
        }
    }
    // 低分辨率附件保存未调制颜色；最终阶段只应用一次颜色/透明度调制。
    fragColor = ResolvePass == 0 ? total
        : vec4(total.rgb * ColorModulator.rgb * ColorModulator.a, total.a * ColorModulator.a);
}

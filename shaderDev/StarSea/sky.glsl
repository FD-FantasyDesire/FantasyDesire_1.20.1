// 方向空间的连续星空与局部程序化星云。地面/墙面只决定可见遮罩，不参与星图 UV。
vec3 skyHash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yzz) * p.zyx);
}

float skyNoise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(skyHash(i).x, skyHash(i + vec3(1,0,0)).x, u.x),
                   mix(skyHash(i + vec3(0,1,0)).x, skyHash(i + vec3(1,1,0)).x, u.x), u.y),
               mix(mix(skyHash(i + vec3(0,0,1)).x, skyHash(i + vec3(1,0,1)).x, u.x),
                   mix(skyHash(i + vec3(0,1,1)).x, skyHash(i + vec3(1,1,1)).x, u.x), u.y), u.z);
}

float skyFbm(vec3 p) {
    float value = 0.0, weight = 0.5, total = 0.0;
    for (int i = 0; i < 4; i++) {
        value += skyNoise(p) * weight;
        total += weight;
        p = p.yzx * 2.03 + vec3(13.7, 7.9, 19.3);
        weight *= 0.5;
    }
    return value / total;
}

vec3 directionGalaxy(vec3 ray, out float transmission) {
    vec3 axis = normalize(vec3(0.30, 0.35, -0.78));
    vec3 seed = vec3(SEED * 1.73, SEED * 0.91, 5.3);
    float warp = skyFbm(ray * 3.1 + seed);
    float clouds = skyFbm(ray * 9.0 + seed + warp * 0.8);
    float detail = skyFbm(ray * 23.0 + seed + warp);
    float latitude = dot(ray, axis) + (warp - 0.5) * 0.19;
    float band = exp(-latitude * latitude / 0.019);
    float outer = exp(-latitude * latitude / 0.12);
    float dustAxis = latitude + 0.012 + (detail - 0.5) * 0.085;
    float dust = exp(-dustAxis * dustAxis / 0.0009) * smoothstep(0.3, 0.63, clouds + detail * 0.18);
    transmission = 1.0 - dust * 0.78;
    vec3 gas = mix(vec3(0.030,0.060,0.125), vec3(0.110,0.043,0.105),
        smoothstep(-0.55, 0.55, ray.x + warp - 0.5));
    vec3 light = gas * band * pow(clamp(clouds * 1.3, 0.0, 1.0), 2.4) * (0.4 + detail * 1.5);
    light += vec3(0.022,0.028,0.048) * outer * clouds * 0.35;
    light += vec3(0.12,0.095,0.075) * band * pow(clouds, 5.0) * 0.9;
    return light * (1.0 - dust * 0.9) * NEBULA_GAIN;
}

vec3 nebulaPalette(float value) {
    // 七段循环调色板：深蓝、天蓝、橙、青绿、深红、紫、黄。
    float t = fract(value);
    if (t < 0.142857) return mix(vec3(0.010, 0.035, 0.15), vec3(0.08, 0.42, 0.95), t * 7.0);
    if (t < 0.285714) return mix(vec3(0.08, 0.42, 0.95), vec3(0.92, 0.27, 0.055), (t - 0.142857) * 7.0);
    if (t < 0.428571) return mix(vec3(0.92, 0.27, 0.055), vec3(0.02, 0.66, 0.52), (t - 0.285714) * 7.0);
    if (t < 0.571428) return mix(vec3(0.02, 0.66, 0.52), vec3(0.34, 0.012, 0.045), (t - 0.428571) * 7.0);
    if (t < 0.714285) return mix(vec3(0.34, 0.012, 0.045), vec3(0.52, 0.075, 0.72), (t - 0.571428) * 7.0);
    if (t < 0.857142) return mix(vec3(0.52, 0.075, 0.72), vec3(0.90, 0.69, 0.09), (t - 0.714285) * 7.0);
    return mix(vec3(0.90, 0.69, 0.09), vec3(0.010, 0.035, 0.15), (t - 0.857142) * 7.0);
}

float nebulaPaletteSlot(float value) {
    // 将颜色锁在七段调色板的中心，保证大多数云团呈稳定纯色。
    return (floor(fract(value) * 7.0) + 0.5) / 7.0;
}

// 观察方向上的程序化星云簇。输入只使用单位射线，因此不会取样地形表面，
// 也不会绑定世界坐标；时间只驱动轻微的云丝漂移。
vec3 directionNebulaClusters(vec3 ray, float seconds, float seed, float gain) {
    vec3 seedOffset = vec3(seed * 0.173, seed * 0.071, seed * 0.113);
    vec3 drift = vec3(seconds * 0.018, -seconds * 0.013, seconds * 0.015);
    vec3 p = ray * 3.8 + seedOffset + drift;
    float macro = skyFbm(p * 0.72 + vec3(7.1, 19.3, 3.7));
    float cloud = skyFbm(p * 1.85 + vec3(31.7, -11.4, 23.1));
    float filaments = skyFbm(p * 4.6 + vec3(-13.2, 27.4, 8.5));
    float clusters = smoothstep(0.46, 0.72, macro) * smoothstep(0.34, 0.78, cloud);
    float wisps = smoothstep(0.40, 0.76, filaments) * clusters;
    float paletteNoise = skyFbm(p * 0.34 + vec3(17.2, -4.6, 29.1));
    float modeNoise = skyFbm(p * 0.52 + vec3(-9.8, 23.7, 5.4));
    vec3 color = nebulaPalette(nebulaPaletteSlot(paletteNoise + seed * 0.0017));
    vec3 accent = nebulaPalette(nebulaPaletteSlot(paletteNoise + 0.37 + cloud * 0.11));
    vec3 third = nebulaPalette(nebulaPaletteSlot(paletteNoise + 0.69 + filaments * 0.13));
    float blend = clamp(cloud * 0.62 + filaments * 0.38, 0.0, 1.0);
    // 75% 单色、20% 混 2 色、5% 混 3 色；阈值由低频噪声决定，按云团而非像素变化。
    if (modeNoise >= 0.75 && modeNoise < 0.95) {
        color = mix(color, accent, blend);
    } else if (modeNoise >= 0.95) {
        color = mix(mix(color, accent, blend), third, smoothstep(0.32, 0.88, filaments));
    }
    float luminousCore = smoothstep(0.66, 0.90, macro + cloud * 0.18) * wisps;
    return (color * (clusters * 0.34 + wisps * 0.42)
        + accent * luminousCore * 0.22) * max(gain, 0.0);
}

// 候选点只在半径 scale 的薄层中生成，再归一化到方向球面。
// 这是恒星的方向索引，不绘制球壳。径向误差 <= 0.3、光斑半径 <= 0.4 格，
// 所以相邻 3×3×3 格可覆盖全部贡献，不会在网格或经纬方向产生接缝。
vec3 directionStars(vec3 ray, float scale, float occupancy, float radius,
        float brightness, float salt, float glints, float pixel, float seconds) {
    vec3 cell = floor(ray * scale);
    float aa = min(pixel * scale * 0.55, 0.10);
    vec3 color = vec3(0.0);
    for (int z = -1; z <= 1; z++) for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
        vec3 id = cell + vec3(float(x), float(y), float(z));
        vec3 random = skyHash(id + vec3(salt, SEED, salt * 0.73));
        vec3 point = id + 0.12 + random * 0.76;
        float radial = length(point);
        if (abs(radial - scale) > 0.30) continue;
        vec3 detail = skyHash(id + vec3(SEED * 3.17, salt + 42.6, 17.8));
        if (detail.z >= occupancy * STAR_DENSITY) continue;
        vec3 direction = point / max(radial, 0.001);
        vec3 delta = (ray - direction) * scale;
        float d2 = dot(delta, delta);
        if (d2 >= 0.16) continue;
        float r = radius * mix(0.65, 1.35, detail.x);
        float filtered2 = r * r + aa * aa;
        float core = exp(-d2 / filtered2) * r * r / filtered2;
        float halo = exp(-sqrt(d2) / max(r * 3.8, 0.001)) * mix(0.008, 0.055, glints);
        float rays = 0.0;
        if (glints > 0.0 && detail.x > 0.72) {
            vec3 reference = abs(direction.y) < 0.9 ? vec3(0,1,0) : vec3(1,0,0);
            vec3 tangent = normalize(cross(direction, reference));
            vec2 a = abs(vec2(dot(delta, tangent), dot(delta, cross(direction, tangent))));
            float width2 = r * r * 0.16 + aa * aa;
            rays = (exp(-a.x * a.x / width2 - a.y / 0.115)
                + exp(-a.y * a.y / width2 - a.x / 0.115)) * r * 0.4 / sqrt(width2) * 0.23;
        }
        float frequency = mix(0.55, 1.45, detail.y);
        float wave = sin(seconds * frequency + random.z * TAU) * sin(seconds * frequency * 0.37 + random.x * TAU);
        float support = 1.0 - smoothstep(0.28, 0.40, sqrt(d2));
        color += starColor(random.z) * (core + halo + rays) * support * brightness
            * mix(0.35, 1.0, detail.x * detail.x) * (1.0 + TWINKLE * wave);
    }
    return color;
}

// 六个方向星区分别生成流星；所有轨迹都在同一世界方向球面上计算，跨面无接缝。
vec3 directionMeteors(vec3 ray, float seconds, float pixel) {
    float rate = clamp(MeteorRate, 0.0, 2.0);
    if (rate <= 0.0) return vec3(0.0);
    vec3 color = vec3(0.0);
    for (int region = 0; region < 6; region++) {
        float time = seconds + float(region) * 0.37;
        float newest = floor(time * rate);
        vec3 axis = region < 2 ? vec3(1,0,0) : (region < 4 ? vec3(0,1,0) : vec3(0,0,1));
        axis *= mod(float(region), 2.0) < 0.5 ? 1.0 : -1.0;
        vec3 reference = abs(axis.y) < 0.9 ? vec3(0,1,0) : vec3(1,0,0);
        vec3 right = normalize(cross(axis, reference)), up = cross(right, axis);
        for (int i = 0; i < 4; i++) {
            float eventId = newest - float(i);
            float age = time - eventId / rate;
            if (eventId < 0.0 || age <= 0.0 || age >= 1.65) continue;
            vec3 random = hash23(vec2(eventId + SEED, float(region) * 17.3 + 123.4));
            vec3 start = normalize(axis + right * mix(-0.7, 0.25, random.x) + up * mix(-0.4, 0.7, random.y));
            vec3 tangent = normalize(right - up * 0.55 - start * dot(right - up * 0.55, start));
            vec3 side = cross(start, tangent);
            float headAngle = age * 0.25;
            float forward = dot(ray, start);
            // 背半球和极点没有此条流星，先排除 atan(0,0) 的退化方向。
            if (forward < 0.60) continue;
            float along = atan(dot(ray, tangent), forward) - headAngle;
            float across = abs(dot(ray, side));
            if (along < -0.20 || along > 0.025 || across > 0.025) continue;
            float tail = 0.14 * smoothstep(0.0, 0.20, age);
            float width = max(0.0007, pixel * 0.65);
            float body = exp(-across * across / (width * width)) * exp(min(along, 0.0) / 0.045)
                * smoothstep(-tail - width, -tail + width, along) * (1.0 - smoothstep(-width, width, along));
            vec3 head = start * cos(headAngle) + tangent * sin(headAngle);
            float core = exp(-dot(ray - head, ray - head) / (width * width * 2.0));
            float life = smoothstep(0.0, 0.14, age) * (1.0 - smoothstep(0.95, 1.65, age));
            color += mix(vec3(0.51,0.73,1.0), vec3(0.89,0.75,1.0), random.z) * (body * 1.2 + core * 1.6) * life;
        }
    }
    return color;
}

vec3 continuousSky(vec3 ray, float pixel, float seconds, float nebulaSeed, float nebulaGain) {
    float transmission;
    vec3 color = vec3(0.0016,0.0026,0.0065) + directionGalaxy(ray, transmission);
    color += directionNebulaClusters(ray, seconds, nebulaSeed, nebulaGain);
    float fine = 1.0 - smoothstep(0.003, 0.009, pixel);
    color += directionStars(ray, 100.0, 0.16, 0.038, 1.3, 17.0, 0.0, pixel, seconds) * transmission * fine;
    color += directionStars(ray, 48.0, 0.19, 0.032, 2.0, 53.0, 0.0, pixel, seconds) * mix(1.0, transmission, 0.35);
    color += directionStars(ray, 22.0, 0.20, 0.027, 3.6, 97.0, 1.0, pixel, seconds);
    color += directionMeteors(ray, seconds, pixel);
    return color;
}

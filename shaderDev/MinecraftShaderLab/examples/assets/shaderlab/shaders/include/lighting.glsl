#version 150
vec3 lab_light(vec3 normal, vec3 a, vec3 b) {
    vec3 n = normalize(normal);
    float diffuse = min(1.0, 0.4 + 0.6 * (max(dot(a, n), 0.0) + max(dot(b, n), 0.0)));
    return vec3(diffuse);
}

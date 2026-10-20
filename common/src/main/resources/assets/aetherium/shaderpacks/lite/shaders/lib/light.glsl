// Vanilla's lightmap colour, warmed: torches glow orange, sunlit surfaces get a soft warm cast.
vec3 liteLight(vec3 vanilla, vec2 lm) {
    float torch = clamp((lm.x - 0.03125) * 1.07, 0.0, 1.0);
    float sky = clamp((lm.y - 0.03125) * 1.07, 0.0, 1.0);
    vec3 l = vanilla;
    l += vec3(1.0, 0.5, 0.18) * (torch * torch * torch) * 0.22 * TORCH_WARMTH;
    l *= mix(vec3(1.0), vec3(1.06, 1.01, 0.93), sky * 0.7 * WARMTH);
    return l;
}

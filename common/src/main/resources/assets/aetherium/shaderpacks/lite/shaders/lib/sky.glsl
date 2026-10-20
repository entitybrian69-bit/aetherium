// Aetherium Lite sky. Shared by the sky and the fog, so far terrain melts into the sky instead of
// ending at a hard band. Built from vanilla's sky and fog colors, so time of day, weather, biomes
// and the other dimensions stay right.
uniform vec3 skyColor;
uniform vec3 fogColor;
uniform vec3 sunPosition;
uniform vec3 upPosition;
uniform float rainStrength;

float liteDaylight() {
    return smoothstep(-0.12, 0.22, dot(normalize(sunPosition), normalize(upPosition)));
}

// dir: normalized view-space direction
vec3 liteSky(vec3 dir) {
    vec3 up = normalize(upPosition);
    float h = dot(dir, up);
#ifdef SKY_FX
    vec3 sunDir = normalize(sunPosition);
    float sunUp = dot(sunDir, up);
    float day = smoothstep(-0.12, 0.22, sunUp);
    // a deeper, more saturated zenith than vanilla's flat color; the horizon comes from the fog color
    vec3 zenith = pow(max(skyColor, vec3(0.0)), vec3(1.45)) * 1.08 + vec3(0.004, 0.006, 0.014);
    float fogLuma = dot(fogColor, vec3(0.299, 0.587, 0.114));
    vec3 horizon = mix(vec3(fogLuma), fogColor, 1.15) * (1.0 + 0.25 * HORIZON_GLOW * day);
    vec3 c = mix(horizon, zenith, smoothstep(-0.02, 0.5, h));
    // sunrise and sunset: a warm band on the sun's side of the horizon
    vec3 dirFlat = normalize(dir - up * h + vec3(0.0001));
    vec3 sunFlat = normalize(sunDir - up * sunUp + vec3(0.0001));
    float side = dot(dirFlat, sunFlat) * 0.5 + 0.5;
    float dusk = 1.0 - smoothstep(0.0, 0.32, abs(sunUp + 0.03));
    float band = dusk * side * side * (1.0 - smoothstep(-0.05, 0.38, h)) * (1.0 - rainStrength);
    c = mix(c, vec3(1.0, 0.48, 0.2) * max(0.55, day + 0.35), band * 0.8);
    // glow around the sun
    float s = max(dot(dir, sunDir), 0.0);
    float glow = pow(s, 6.0 / max(SUN_GLOW, 0.25)) * 0.1 + pow(s, 40.0) * 0.28;
    c += vec3(1.0, 0.84, 0.6) * glow * SUN_GLOW * (day * 0.8 + dusk * 0.6) * (1.0 - rainStrength);
    // below the horizon a little darker, rain greys everything out
    c *= mix(1.0, 0.7, smoothstep(0.0, -0.35, h));
    float g = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(c, vec3(g * 0.9), rainStrength * 0.65);
    return max(c, vec3(0.0));
#else
    return mix(fogColor, skyColor, smoothstep(-0.02, 0.5, h));
#endif
}

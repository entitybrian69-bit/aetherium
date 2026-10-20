// Aetherium Lite fog. Computed here instead of from gl_Fog, which some GL translators (Krypton on
// Android) never fill in, so terrain drew to a hard edge there. Fades into Lite's sky.
#include "/lib/sky.glsl"
uniform float far;
uniform int isEyeInWater;
uniform float blindness;
uniform ivec2 eyeBrightnessSmooth;
varying vec3 liteViewPos;

vec3 applyFog(vec3 color) {
    float d = length(liteViewPos);
    if (isEyeInWater == 1) {
        return mix(color, fogColor * 0.9, 1.0 - exp(-d * 0.075));
    }
    if (isEyeInWater == 2) {
        return mix(color, fogColor, clamp(d * 0.6, 0.0, 1.0));
    }
    float f = smoothstep(far * FOG_START, far * 0.98, d);
    // aerial perspective: distant hills turn slightly hazy and blue, like real air
    f = max(f, (1.0 - exp(-d / max(far * 3.0, 48.0))) * ATMOSPHERE * liteDaylight());
    f = clamp(f + rainStrength * smoothstep(0.0, far * 0.6, d) * 0.45, 0.0, 1.0);
    // open sky: the sky in that direction; underground and in the Nether: vanilla's fog color
    float open = smoothstep(0.0, 180.0, float(eyeBrightnessSmooth.y));
    vec3 fc = mix(fogColor, liteSky(normalize(liteViewPos)), open);
    color = mix(color, fc, f);
    return mix(color, vec3(0.0), clamp(blindness * smoothstep(0.0, 6.0, d), 0.0, 1.0));
}

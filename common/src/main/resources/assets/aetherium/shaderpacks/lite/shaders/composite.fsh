#version 120
#include "/settings.glsl"

uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform float viewWidth;
uniform float viewHeight;
uniform vec3 sunPosition;
uniform vec3 shadowLightPosition;
uniform mat4 gbufferProjection;
uniform float rainStrength;
uniform int isEyeInWater;

varying vec2 texcoord;

void main() {
    vec3 c = texture2D(colortex0, texcoord).rgb;
#ifdef BLOOM
    // only real highlights glow (lamps, lava, the sun, sunlit snow): the daytime sky stays below the knee
    vec2 px = 1.0 / vec2(viewWidth, viewHeight);
    vec3 glow = vec3(0.0);
    for (int i = 0; i < 12; i++) {
        float a = float(i) * 0.5236;
        vec2 o = vec2(cos(a), sin(a));
        vec3 s1 = texture2D(colortex0, texcoord + o * px * 5.0).rgb;
        vec3 s2 = texture2D(colortex0, texcoord + o * px * 12.0).rgb;
        glow += s1 * smoothstep(0.78, 1.0, dot(s1, vec3(0.2126, 0.7152, 0.0722)));
        glow += s2 * smoothstep(0.78, 1.0, dot(s2, vec3(0.2126, 0.7152, 0.0722))) * 0.6;
    }
    c += glow * (BLOOM_STRENGTH / 24.0);
#endif
#ifdef GODRAYS
    vec4 sp = gbufferProjection * vec4(shadowLightPosition, 1.0);
    if (sp.w > 0.0 && isEyeInWater == 0) {
        vec2 light = sp.xy / sp.w * 0.5 + 0.5;
        vec2 stepUv = (texcoord - light) / 16.0;
        vec2 uv = texcoord;
        float sky = 0.0;
        for (int i = 0; i < 16; i++) {
            uv -= stepUv;
            if (uv.x > 0.0 && uv.x < 1.0 && uv.y > 0.0 && uv.y < 1.0) {
                sky += step(0.9999, texture2D(depthtex0, uv).r);
            }
        }
        bool day = dot(normalize(sunPosition), normalize(shadowLightPosition)) > 0.5;
        float fade = clamp(1.0 - length((light - 0.5) * vec2(viewWidth / viewHeight, 1.0)) * 0.9, 0.0, 1.0);
        // shafts fall off quickly away from the light and stay subtle (the sky must not wash out)
        float near = exp(-length((texcoord - light) * vec2(viewWidth / viewHeight, 1.0)) * 3.0);
        vec3 tint = day ? vec3(1.0, 0.86, 0.62) : vec3(0.45, 0.55, 0.8) * 0.4;
        c += tint * (sky / 16.0) * near * fade * 0.18 * GODRAY_STRENGTH * (1.0 - rainStrength);
    }
#endif
/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(c, 1.0);
}

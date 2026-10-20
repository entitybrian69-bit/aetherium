#version 120
#include "/settings.glsl"
#include "/lib/fog.glsl"
#include "/lib/light.glsl"
#ifdef SHADOWS
#include "/lib/shadow.glsl"
uniform sampler2D shadowtex0;
varying vec4 shadowClip;
varying float lightFacing;
#endif

uniform sampler2D texture;
uniform sampler2D lightmap;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    if (color.a < 0.1) {
        discard; // cutout leaves and plants (GL ES has no fixed-function alpha test)
    }
    vec2 lm = lmcoord;
#ifdef SHADOWS
    float lit = 1.0;
    if (lightFacing <= 0.0) {
        lit = 0.0;
    } else {
        vec3 p = distortShadow(shadowClip.xyz / shadowClip.w) * 0.5 + 0.5;
        if (p.x > 0.0 && p.x < 1.0 && p.y > 0.0 && p.y < 1.0 && p.z < 1.0) {
            lit = step(p.z - 0.0008, texture2D(shadowtex0, p.xy).r);
        }
    }
    // shadowed parts lose part of their sky light (block light is untouched)
    lm.y = mix(lm.y * SHADOW_DARKNESS, lm.y, lit);
    lm.y = max(lm.y, 0.03125);
#endif
    color.rgb *= liteLight(texture2D(lightmap, lm).rgb, lm);
    color.rgb = applyFog(color.rgb);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}

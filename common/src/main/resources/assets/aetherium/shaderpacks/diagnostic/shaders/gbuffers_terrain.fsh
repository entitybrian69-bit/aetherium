#version 120
#include "/settings.glsl"
#ifdef SHADOW_VIEW
#include "/lib/shadow.glsl"
uniform sampler2D shadowtex0;
varying vec4 shadowClip;
varying float lightFacing;
#endif

uniform sampler2D texture;

varying vec2 texcoord;
varying vec4 glcolor;
varying vec3 worldNormal;
varying float blockId;
varying float midY;

void main() {
    vec4 c = texture2D(texture, texcoord) * glcolor;
    if (c.a < 0.1) {
        discard;
    }
    float luma = dot(c.rgb, vec3(0.3, 0.6, 0.1));
#ifdef SHADOW_VIEW
    vec3 shown = vec3(0.5);
    if (lightFacing <= 0.0) {
        shown = vec3(0.0, 0.0, 0.35);
    } else {
        vec3 p = distortShadow(shadowClip.xyz / shadowClip.w) * 0.5 + 0.5;
        if (p.x > 0.0 && p.x < 1.0 && p.y > 0.0 && p.y < 1.0 && p.z < 1.0) {
            shown = vec3(step(p.z - 0.0008, texture2D(shadowtex0, p.xy).r));
        }
    }
    c.rgb = shown * (0.75 + 0.25 * luma);
#else
    if (blockId > 10000.5 && blockId < 10003.5) {
        c.rgb = midY > 0.0001 ? vec3(1.0, 0.0, 0.0) : vec3(1.0, 1.0, 0.0);
    } else if (abs(blockId - 10008.0) < 0.5) {
        c.rgb = vec3(0.0, 0.0, 1.0);
    } else {
        c.rgb = (normalize(worldNormal) * 0.5 + 0.5) * (0.6 + 0.4 * luma);
    }
#endif
/* DRAWBUFFERS:0 */
    gl_FragData[0] = c;
}

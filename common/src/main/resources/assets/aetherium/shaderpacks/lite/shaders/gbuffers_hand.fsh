#version 120
#include "/settings.glsl"
#include "/lib/fog.glsl"
#include "/lib/light.glsl"
uniform sampler2D texture;
uniform sampler2D lightmap;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    color.rgb *= liteLight(texture2D(lightmap, lmcoord).rgb, lmcoord);
    color.rgb = applyFog(color.rgb);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}

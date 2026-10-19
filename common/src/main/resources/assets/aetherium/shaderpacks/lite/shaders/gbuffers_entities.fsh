#version 120
#include "/lib/fog.glsl"
uniform sampler2D texture;
uniform sampler2D lightmap;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor * texture2D(lightmap, lmcoord);
    color.rgb = applyFog(color.rgb);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}

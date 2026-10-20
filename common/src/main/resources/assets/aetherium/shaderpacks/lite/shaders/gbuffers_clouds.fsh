#version 120
#include "/settings.glsl"
#include "/lib/fog.glsl"
uniform sampler2D texture;
varying vec2 texcoord;
varying vec4 glcolor;
void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    color.rgb *= CLOUD_BRIGHTNESS;
    color.rgb = applyFog(color.rgb);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}

#version 120
#include "/lib/fog.glsl"
varying vec4 glcolor;
void main() {
/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(applyFog(glcolor.rgb), glcolor.a);
}

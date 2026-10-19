#version 120
#include "/lib/shadow.glsl"
varying vec2 texcoord;
varying vec4 glcolor;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
    vec4 p = ftransform();
    gl_Position = vec4(distortShadow(p.xyz / p.w), 1.0);
}

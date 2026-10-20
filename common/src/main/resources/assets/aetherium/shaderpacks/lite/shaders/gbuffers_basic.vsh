#version 120
#include "/lib/fogv.glsl"
varying vec4 glcolor;
void main() {
    glcolor = gl_Color;
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    gl_Position = gl_ProjectionMatrix * vp;
    liteViewPos = vp.xyz;
}

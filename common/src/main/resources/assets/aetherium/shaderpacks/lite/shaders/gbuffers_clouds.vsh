#version 120
varying vec2 texcoord;
varying vec4 glcolor;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    gl_Position = gl_ProjectionMatrix * vp;
    gl_FogFragCoord = length(vp.xyz);
}

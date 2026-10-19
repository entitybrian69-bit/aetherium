#version 120
varying vec4 glcolor;
void main() {
    glcolor = gl_Color;
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    gl_Position = gl_ProjectionMatrix * vp;
    gl_FogFragCoord = length(vp.xyz);
}

#version 120
varying vec4 glcolor;
varying vec3 viewPos;
void main() {
    glcolor = gl_Color;
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    viewPos = vp.xyz;
    gl_Position = gl_ProjectionMatrix * vp;
    gl_FogFragCoord = length(vp.xyz);
}

#version 120
#include "/settings.glsl"

attribute vec4 mc_Entity;

uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 worldPos;
varying vec3 normal;
varying float isWater;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    viewPos = vp.xyz;
    worldPos = (gbufferModelViewInverse * vp).xyz + cameraPosition;
    normal = normalize(gl_NormalMatrix * gl_Normal);
    isWater = abs(mc_Entity.x - 10008.0) < 0.5 ? 1.0 : 0.0;
    gl_Position = gl_ProjectionMatrix * vp;
    gl_FogFragCoord = length(vp.xyz);
}

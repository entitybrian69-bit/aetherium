#version 120
#include "/settings.glsl"

attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

uniform mat4 gbufferModelViewInverse;
#ifdef SHADOW_VIEW
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
uniform vec3 shadowLightPosition;
varying vec4 shadowClip;
varying float lightFacing;
#endif

varying vec2 texcoord;
varying vec4 glcolor;
varying vec3 worldNormal;
varying float blockId;
varying float midY;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
    blockId = mc_Entity.x;
    midY = mc_midTexCoord.y;
    vec3 normal = normalize(gl_NormalMatrix * gl_Normal);
    worldNormal = mat3(gbufferModelViewInverse) * normal;
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    gl_Position = gl_ProjectionMatrix * viewPos;
#ifdef SHADOW_VIEW
    lightFacing = dot(normal, normalize(shadowLightPosition));
    vec4 playerPos = gbufferModelViewInverse * viewPos;
    vec4 biased = playerPos + vec4(worldNormal * 0.06, 0.0);
    shadowClip = shadowProjection * (shadowModelView * biased);
#endif
}

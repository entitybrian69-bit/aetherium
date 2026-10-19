#version 120
#include "/settings.glsl"

attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

uniform float frameTimeCounter;
uniform vec3 cameraPosition;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
#ifdef SHADOWS
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
uniform vec3 shadowLightPosition;
varying vec4 shadowClip;
varying float lightFacing;
#endif

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 playerPos = gbufferModelViewInverse * viewPos;
#ifdef WAVING_PLANTS
    float id = mc_Entity.x;
    if (id > 10000.5 && id < 10003.5) {
        bool top = texcoord.y < mc_midTexCoord.y;
        float strength = id > 10002.5 ? 0.035 : (id > 10001.5 || top ? 0.09 : 0.0);
        if (strength > 0.0) {
            vec3 world = playerPos.xyz + cameraPosition;
            float t = frameTimeCounter * WAVE_SPEED;
            playerPos.x += sin(t * 1.7 + world.x * 0.6 + world.z * 0.4) * strength;
            playerPos.z += cos(t * 1.3 + world.z * 0.6 + world.x * 0.3) * strength * 0.8;
            viewPos = gbufferModelView * playerPos;
        }
    }
#endif
    gl_Position = gl_ProjectionMatrix * viewPos;
    gl_FogFragCoord = length(viewPos.xyz);
#ifdef SHADOWS
    vec3 normal = normalize(gl_NormalMatrix * gl_Normal);
    lightFacing = dot(normal, normalize(shadowLightPosition));
    // nudge along the normal against shadow acne
    vec4 biased = playerPos + vec4(mat3(gbufferModelViewInverse) * normal * 0.06, 0.0);
    shadowClip = shadowProjection * (shadowModelView * biased);
#endif
}

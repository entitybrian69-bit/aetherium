#version 120
#include "/settings.glsl"

attribute vec4 mc_Entity;

uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferModelView;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

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
    normal = normalize(gl_NormalMatrix * gl_Normal);
    // block id 10008, or (without an id) the blue biome tint only water gets in the translucent layer
    isWater = abs(mc_Entity.x - 10008.0) < 0.5 || (mc_Entity.x < 0.5 && gl_Color.b > gl_Color.r + 0.15) ? 1.0 : 0.0;
    vec4 playerPos = gbufferModelViewInverse * vp;
    worldPos = playerPos.xyz + cameraPosition;
#ifdef WATER_FX
#ifdef WAVING_WATER
    if (isWater > 0.5) {
        float t = frameTimeCounter * WAVE_SPEED;
        playerPos.y += (sin(t * 1.4 + worldPos.x * 0.7 + worldPos.z * 0.3) * 0.5
                + sin(t * 1.9 - worldPos.z * 0.9 + worldPos.x * 0.2) * 0.5) * 0.035 * max(WATER_WAVES, 0.3) - 0.03;
        vp = gbufferModelView * playerPos;
    }
#endif
#endif
    viewPos = vp.xyz;
    gl_Position = gl_ProjectionMatrix * vp;
    gl_FogFragCoord = length(vp.xyz);
}

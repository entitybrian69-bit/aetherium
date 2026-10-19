#version 120
#include "/settings.glsl"
#include "/lib/fog.glsl"

uniform sampler2D texture;
uniform sampler2D lightmap;
uniform vec3 skyColor;
uniform vec3 shadowLightPosition;
uniform float frameTimeCounter;
uniform float rainStrength;
uniform mat4 gbufferModelView;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 worldPos;
varying vec3 normal;
varying float isWater;

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    vec4 light = texture2D(lightmap, lmcoord);
#ifdef WATER_FX
    if (isWater > 0.5) {
        float t = frameTimeCounter * WAVE_SPEED;
        vec2 p = worldPos.xz;
        // two crossing wave trains perturb the surface normal (in world space, then to view space)
        float dx = cos(p.x * 1.3 + t * 1.6) * 0.06 + cos((p.x + p.y) * 2.1 + t * 2.3) * 0.04;
        float dz = cos(p.y * 1.1 - t * 1.4) * 0.06 + cos((p.x - p.y) * 1.9 + t * 2.0) * 0.04;
        vec3 n = normalize(mat3(gbufferModelView) * normalize(vec3(-dx, 1.0, -dz)));
        if (dot(n, normal) < 0.2) {
            n = normal;
        }
        vec3 v = normalize(viewPos);
        float fresnel = pow(1.0 - clamp(dot(-v, n), 0.0, 1.0), 4.0);
        float skyLight = clamp((lmcoord.y - 0.03125) * 1.07, 0.0, 1.0);
        vec3 reflection = mix(skyColor * 0.6, skyColor * 1.2, fresnel) * skyLight;
        color.rgb = mix(color.rgb * light.rgb, reflection, fresnel * 0.75);
        vec3 r = reflect(v, n);
        float spec = pow(max(dot(r, normalize(shadowLightPosition)), 0.0), 120.0) * (1.0 - rainStrength);
        color.rgb += vec3(1.0, 0.92, 0.75) * spec * 2.0 * skyLight;
        color.a = mix(0.62, 0.95, fresnel);
    } else {
        color *= light;
    }
#else
    color *= light;
#endif
    color.rgb = applyFog(color.rgb);
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}

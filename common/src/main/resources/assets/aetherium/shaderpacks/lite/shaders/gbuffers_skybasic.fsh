#version 120
#include "/settings.glsl"
uniform vec3 sunPosition;
uniform vec3 upPosition;
uniform vec3 fogColor;
uniform float rainStrength;
varying vec4 glcolor;
varying vec3 viewPos;
void main() {
    vec4 c = glcolor;
#ifdef SKY_FX
    vec3 v = normalize(viewPos);
    float up = dot(v, normalize(upPosition));
    float sunUp = dot(normalize(sunPosition), normalize(upPosition));
    float day = smoothstep(-0.1, 0.25, sunUp);
    // a brighter band toward the horizon and a soft glow around the sun (daytime, clear weather)
    float horizon = pow(1.0 - clamp(abs(up), 0.0, 1.0), 4.0);
    c.rgb = mix(c.rgb, fogColor * 1.08, horizon * HORIZON_GLOW * day);
    float sun = max(dot(v, normalize(sunPosition)), 0.0);
    c.rgb += vec3(1.0, 0.82, 0.55) * (pow(sun, 12.0 / max(SUN_GLOW, 0.25)) * 0.18 + pow(sun, 64.0) * 0.12) * SUN_GLOW * day * (1.0 - rainStrength);
#endif
/* DRAWBUFFERS:0 */
    gl_FragData[0] = c;
}

#version 120
#include "/settings.glsl"

uniform sampler2D colortex0;
varying vec2 texcoord;

void main() {
    vec3 c = texture2D(colortex0, texcoord).rgb * (EXPOSURE * 1.1);
    // filmic curve (ACES fit): richer midtones and contrast, highlights roll off instead of clipping
    c = clamp((c * (2.51 * c + 0.03)) / (c * (2.43 * c + 0.59) + 0.14), 0.0, 1.0);
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = max(mix(vec3(l), c, SATURATION), 0.0);
    // split toning: cool shadows, warm highlights
    vec3 tone = mix(vec3(0.96, 0.99, 1.05), vec3(1.04, 1.0, 0.95), smoothstep(0.15, 0.75, l));
    c *= mix(vec3(1.0), tone, clamp(WARMTH, 0.0, 1.5));
#ifdef VIGNETTE
    float v = smoothstep(0.95, 0.3, length(texcoord - 0.5));
    c *= mix(1.0, v, 0.45);
#endif
    gl_FragColor = vec4(c, 1.0);
}

#version 120
#include "/settings.glsl"

uniform sampler2D colortex0;
varying vec2 texcoord;

void main() {
    vec3 c = texture2D(colortex0, texcoord).rgb * EXPOSURE;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = max(mix(vec3(l), c, SATURATION), 0.0);
    c = c * (1.0 + c * 0.08) / (1.0 + c * 0.12); // gentle highlight roll-off
#ifdef VIGNETTE
    float v = smoothstep(0.95, 0.3, length(texcoord - 0.5));
    c *= mix(1.0, v, 0.4);
#endif
    gl_FragColor = vec4(c, 1.0);
}

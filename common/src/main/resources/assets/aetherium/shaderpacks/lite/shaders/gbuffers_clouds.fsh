#version 120
#include "/settings.glsl"
#include "/lib/fog.glsl"
uniform sampler2D texture;
varying vec2 texcoord;
varying vec4 glcolor;
void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    // whiter, slightly cool clouds (vanilla's tint plus the warm grade read as beige)
    float l = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(color.rgb, vec3(l), 0.35) * vec3(0.97, 0.99, 1.04) * CLOUD_BRIGHTNESS * 1.06;
    // clouds reach far past the render distance: they fade into the sky much later than terrain
    float d = length(liteViewPos);
    vec3 sky = liteSky(normalize(liteViewPos));
    color.rgb = mix(color.rgb, sky, smoothstep(far * 0.9, far * 3.2, d) * 0.9);
    if (isEyeInWater != 0) {
        color.rgb = applyFog(color.rgb);
    }
/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}

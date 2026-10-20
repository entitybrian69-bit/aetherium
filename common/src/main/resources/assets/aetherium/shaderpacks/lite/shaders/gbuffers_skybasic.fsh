#version 120
#include "/settings.glsl"
#include "/lib/sky.glsl"
varying vec4 glcolor;
varying vec3 viewPos;
void main() {
    vec4 c = glcolor;
    // the sky dome is drawn opaque; stars and the sunrise fan are translucent and keep their color
    if (glcolor.a > 0.99) {
        c.rgb = liteSky(normalize(viewPos));
    }
/* DRAWBUFFERS:0 */
    gl_FragData[0] = c;
}

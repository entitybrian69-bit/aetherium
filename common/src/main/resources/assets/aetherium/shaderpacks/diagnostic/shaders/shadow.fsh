#version 120
uniform sampler2D tex;
varying vec2 texcoord;
varying vec4 glcolor;
const int shadowMapResolution = 1024; // Shadow map size. [512 1024 1536 2048]
const float shadowDistance = 64.0; // How far shadows reach. [32.0 48.0 64.0 96.0 128.0]
const float sunPathRotation = -25.0;
void main() {
    vec4 c = texture2D(tex, texcoord) * glcolor;
    if (c.a < 0.1) {
        discard;
    }
    gl_FragData[0] = c;
}

// Vanilla fog (linear above water, exponential in water and lava).
uniform int fogMode;
vec3 applyFog(vec3 color) {
    float f;
    if (fogMode == 2048) {        // GL_EXP2
        float d = gl_Fog.density * gl_FogFragCoord;
        f = exp(-d * d);
    } else if (fogMode == 2049) { // GL_EXP
        f = exp(-gl_Fog.density * gl_FogFragCoord);
    } else {                      // GL_LINEAR
        f = (gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale;
    }
    return mix(gl_Fog.color.rgb, color, clamp(f, 0.0, 1.0));
}

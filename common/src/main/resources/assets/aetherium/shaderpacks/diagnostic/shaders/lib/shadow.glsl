// Shadow map distortion: more resolution near the player.
vec3 distortShadow(vec3 p) {
    float d = length(p.xy) * 0.85 + 0.15;
    return vec3(p.xy / d, p.z * 0.5);
}

uniform mat4 gbufferModelView;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    vec3 n = normalize(gl_NormalMatrix * gl_Normal);
    vec3 l0 = normalize(mat3(gbufferModelView) * vec3(0.2, 1.0, -0.7));
    vec3 l1 = normalize(mat3(gbufferModelView) * vec3(-0.2, 1.0, 0.7));
    float shade = min(1.0, 0.4 + 0.6 * (max(dot(n, l0), 0.0) + max(dot(n, l1), 0.0)));
    glcolor = vec4(gl_Color.rgb * shade, gl_Color.a);
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    gl_Position = gl_ProjectionMatrix * vp;
    gl_FogFragCoord = length(vp.xyz);
}

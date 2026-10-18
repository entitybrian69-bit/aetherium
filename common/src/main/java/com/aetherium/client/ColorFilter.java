package com.aetherium.client;

// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import com.aetherium.util.AetheriumLog;
//~ import com.mojang.blaze3d.pipeline.RenderTarget;
//~ import net.minecraft.client.Minecraft;
//~ import org.lwjgl.opengl.GL20;
// @era:qol16-end

/**
 * Colour filter (1.11.0, Minecraft 1.16.5): a light "shader pack" that grades the finished world
 * picture (saturation, contrast, warmth, brightness and a vignette). One copy of the frame into a
 * spare framebuffer, then the copy is drawn back through a small GLSL 1.10 program, using the
 * same fixed-function blit vanilla presents the frame with, so it runs wherever vanilla's own post
 * effects run (gl4es included). If the driver rejects the program, or anything fails, the filter
 * turns itself off for the session and the picture stays untouched.
 */
public final class ColorFilter {
    private ColorFilter() {
    }

    /**
     * Grading per preset ({@code AetheriumConfig.ShaderPreset} ordinal): saturation, contrast,
     * warmth, vignette, brightness. Row 0 (off) is never used.
     */
    static final float[][] PRESETS = {
        {1.0F, 1.0F, 0.0F, 0.0F, 0.0F},
        {1.35F, 1.12F, 0.0F, 0.15F, 0.0F},     // vibrant
        {1.12F, 1.05F, 0.06F, 0.2F, 0.01F},    // warm
        {0.92F, 1.22F, 0.025F, 0.65F, -0.02F}, // cinematic
    };

    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ private static final AetheriumLog LOGGER = AetheriumLog.of(ColorFilter.class);
    //~ private static final int GL_VERTEX_SHADER = 0x8B31;
    //~ private static final int GL_FRAGMENT_SHADER = 0x8B30;
    //~ private static final int GL_COMPILE_STATUS = 0x8B81;
    //~ private static final int GL_LINK_STATUS = 0x8B82;

    //~ private static final String VERTEX = "#version 110\n"
            //~ + "varying vec2 texCoord;\n"
            //~ + "void main() {\n"
            //~ + "    texCoord = gl_MultiTexCoord0.xy;\n"
            //~ + "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n"
            //~ + "}\n";

    //~ private static final String FRAGMENT = "#version 110\n"
            //~ + "uniform sampler2D DiffuseSampler;\n"
            //~ + "uniform vec4 Grade;\n"
            //~ + "uniform float Brightness;\n"
            //~ + "varying vec2 texCoord;\n"
            //~ + "void main() {\n"
            //~ + "    vec3 c = texture2D(DiffuseSampler, texCoord).rgb;\n"
            //~ + "    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));\n"
            //~ + "    c = mix(vec3(luma), c, Grade.x);\n"
            //~ + "    c = (c - 0.5) * Grade.y + 0.5 + Brightness;\n"
            //~ + "    c += vec3(Grade.z, Grade.z * 0.35, -Grade.z);\n"
            //~ + "    vec2 d = texCoord - 0.5;\n"
            //~ + "    c *= 1.0 - Grade.w * dot(d, d) * 1.6;\n"
            //~ + "    gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);\n"
            //~ + "}\n";

    //~ private static int program;
    //~ private static int gradeLocation;
    //~ private static int brightnessLocation;
    //~ private static boolean broken;
    //~ private static RenderTarget copy;

    //~ /** After the world pass (GameRenderer.render): grade the main framebuffer in place. */
    //~ public static void apply(final Minecraft mc) {
        //~ final int preset = Tuning.shaderPreset;
        //~ if (preset <= 0 || preset >= PRESETS.length || broken) {
            //~ if (copy != null && preset <= 0) {
                //~ release(mc);
            //~ }
            //~ return;
        //~ }
        //~ try {
            //~ if (program == 0 && !compile()) {
                //~ broken = true;
                //~ return;
            //~ }
            //~ final RenderTarget main = mc.getMainRenderTarget();
            //~ if (copy == null) {
                //~ copy = new RenderTarget(main.width, main.height, false, Minecraft.ON_OSX);
            //~ } else if (copy.width != main.width || copy.height != main.height) {
                //~ copy.resize(main.width, main.height, Minecraft.ON_OSX);
            //~ }
            //~ copy.bindWrite(true);
            //~ main.blitToScreen(main.width, main.height, true);
            //~ main.bindWrite(true);
            //~ final float[] g = PRESETS[preset];
            //~ GL20.glUseProgram(program);
            //~ GL20.glUniform4f(gradeLocation, g[0], g[1], g[2], g[3]);
            //~ GL20.glUniform1f(brightnessLocation, g[4]);
            //~ try {
                //~ copy.blitToScreen(main.width, main.height, true);
            //~ } finally {
                //~ GL20.glUseProgram(0);
            //~ }
            //~ main.bindWrite(true);
        //~ } catch (final RuntimeException | LinkageError e) {
            //~ broken = true;
            //~ LOGGER.error("Colour filter turned off after an error", e);
            //~ try {
                //~ release(mc);
            //~ } catch (final RuntimeException | LinkageError ignored) {
                //~ // freed with the GL context
            //~ }
        //~ }
    //~ }

    //~ private static boolean compile() {
        //~ final int vs = shader(GL_VERTEX_SHADER, VERTEX);
        //~ if (vs == 0) {
            //~ return false;
        //~ }
        //~ final int fs = shader(GL_FRAGMENT_SHADER, FRAGMENT);
        //~ if (fs == 0) {
            //~ GL20.glDeleteShader(vs);
            //~ return false;
        //~ }
        //~ final int p = GL20.glCreateProgram();
        //~ GL20.glAttachShader(p, vs);
        //~ GL20.glAttachShader(p, fs);
        //~ GL20.glLinkProgram(p);
        //~ GL20.glDeleteShader(vs);
        //~ GL20.glDeleteShader(fs);
        //~ if (GL20.glGetProgrami(p, GL_LINK_STATUS) == 0) {
            //~ LOGGER.warn("Colour filter: the driver could not link the program: " + GL20.glGetProgramInfoLog(p));
            //~ GL20.glDeleteProgram(p);
            //~ return false;
        //~ }
        //~ GL20.glUseProgram(p);
        //~ GL20.glUniform1i(GL20.glGetUniformLocation(p, "DiffuseSampler"), 0);
        //~ GL20.glUseProgram(0);
        //~ gradeLocation = GL20.glGetUniformLocation(p, "Grade");
        //~ brightnessLocation = GL20.glGetUniformLocation(p, "Brightness");
        //~ program = p;
        //~ return true;
    //~ }

    //~ private static int shader(final int type, final String source) {
        //~ final int s = GL20.glCreateShader(type);
        //~ GL20.glShaderSource(s, source);
        //~ GL20.glCompileShader(s);
        //~ if (GL20.glGetShaderi(s, GL_COMPILE_STATUS) == 0) {
            //~ LOGGER.warn("Colour filter: the driver rejected a shader: " + GL20.glGetShaderInfoLog(s));
            //~ GL20.glDeleteShader(s);
            //~ return 0;
        //~ }
        //~ return s;
    //~ }

    //~ private static void release(final Minecraft mc) {
        //~ if (copy != null) {
            //~ copy.destroyBuffers();
            //~ copy = null;
            //~ mc.getMainRenderTarget().bindWrite(true);
        //~ }
    //~ }
    // @era:qol16-end
}

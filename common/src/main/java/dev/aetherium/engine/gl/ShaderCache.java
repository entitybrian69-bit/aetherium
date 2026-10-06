package dev.aetherium.engine.gl;

import dev.aetherium.Aetherium;
import org.lwjgl.opengl.ARBParallelShaderCompile;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Asynchronous (driver-threaded) program compilation with on-disk program-binary caching.
 * Programs are requested, polled with GL_COMPLETION_STATUS_ARB, and only linked/bound once ready, so
 * no frame ever blocks on glCompileShader.
 */
public final class ShaderCache implements AutoCloseable {
    private static final int GL_COMPLETION_STATUS_ARB = 0x91B1;

    private final Path cacheDir;
    private final boolean parallel;
    private final boolean binary;
    private final Map<String, Pending> pending = new HashMap<>();
    private final Map<String, Integer> ready = new HashMap<>();

    private record Pending(int program, int[] shaders, String key, IntConsumer onReady) {}

    public ShaderCache(Path cacheDir, boolean parallelShaderCompile, boolean programBinary) {
        this.cacheDir = cacheDir;
        this.parallel = parallelShaderCompile;
        this.binary = programBinary;
        if (parallel) {
            ARBParallelShaderCompile.glMaxShaderCompilerThreadsARB(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        }
        try { Files.createDirectories(cacheDir); } catch (IOException e) { Aetherium.LOGGER.warn("[Aetherium] No shader cache dir", e); }
    }

    /** Request a program; {@code onReady} receives the program id on the render thread when it has linked. */
    public void request(String name, Map<Integer, String> stages, IntConsumer onReady) {
        String key = name + "-" + hash(stages);
        Integer cached = ready.get(key);
        if (cached != null) { onReady.accept(cached); return; }

        if (binary) {
            int fromDisk = loadBinary(key);
            if (fromDisk != 0) { ready.put(key, fromDisk); onReady.accept(fromDisk); return; }
        }

        int program = GL20.glCreateProgram();
        int[] shaders = new int[stages.size()];
        int i = 0;
        for (Map.Entry<Integer, String> e : stages.entrySet()) {
            int s = GL20.glCreateShader(e.getKey());
            GL20.glShaderSource(s, e.getValue());
            GL20.glCompileShader(s);
            GL20.glAttachShader(program, s);
            shaders[i++] = s;
        }
        if (binary) GL41.glProgramParameteri(program, GL41.GL_PROGRAM_BINARY_RETRIEVABLE_HINT, GL20.GL_TRUE);
        GL20.glLinkProgram(program);
        pending.put(key, new Pending(program, shaders, key, onReady));
    }

    /** Call once per frame on the render thread. Cheap: only queries completion status. */
    public void poll() {
        if (pending.isEmpty()) return;
        var it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Pending p = it.next().getValue();
            if (parallel && GL20.glGetProgrami(p.program, GL_COMPLETION_STATUS_ARB) == GL20.GL_FALSE) continue;
            it.remove();
            if (GL20.glGetProgrami(p.program, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
                Aetherium.LOGGER.error("[Aetherium] Program '{}' failed to link:\n{}", p.key, GL20.glGetProgramInfoLog(p.program));
                for (int s : p.shaders) Aetherium.LOGGER.error("  shader log: {}", GL20.glGetShaderInfoLog(s));
                GL20.glDeleteProgram(p.program);
                continue;
            }
            for (int s : p.shaders) { GL20.glDetachShader(p.program, s); GL20.glDeleteShader(s); }
            ready.put(p.key, p.program);
            if (binary) storeBinary(p.key, p.program);
            p.onReady.accept(p.program);
        }
    }

    private int loadBinary(String key) {
        Path f = cacheDir.resolve(key + ".bin");
        if (!Files.isRegularFile(f)) return 0;
        try (FileChannel ch = FileChannel.open(f, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocateDirect((int) ch.size());
            ch.read(buf);
            buf.flip();
            int format = buf.getInt();
            int program = GL20.glCreateProgram();
            GL41.glProgramBinary(program, format, buf);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL20.GL_TRUE) return program;
            GL20.glDeleteProgram(program); // driver changed; stale binary
            Files.deleteIfExists(f);
        } catch (IOException e) {
            Aetherium.LOGGER.debug("[Aetherium] Shader binary load failed for {}", key, e);
        }
        return 0;
    }

    private void storeBinary(String key, int program) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int len = GL20.glGetProgrami(program, GL41.GL_PROGRAM_BINARY_LENGTH);
            if (len <= 0) return;
            IntBuffer length = stack.mallocInt(1);
            IntBuffer format = stack.mallocInt(1);
            ByteBuffer data = ByteBuffer.allocateDirect(len + 4);
            data.position(4);
            GL41.glGetProgramBinary(program, length, format, data);
            data.putInt(0, format.get(0));
            data.position(0).limit(4 + length.get(0));
            try (FileChannel ch = FileChannel.open(cacheDir.resolve(key + ".bin"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ch.write(data);
            }
        } catch (IOException e) {
            Aetherium.LOGGER.debug("[Aetherium] Shader binary store failed for {}", key, e);
        }
    }

    private static String hash(Map<Integer, String> stages) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Map.Entry<Integer, String> e : stages.entrySet()) {
                md.update((byte) e.getKey().intValue());
                md.update(e.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(stages.hashCode());
        }
    }

    public static Map<Integer, String> compute(String src) { return Map.of(GL43.GL_COMPUTE_SHADER, src); }

    @Override public void close() {
        for (Pending p : pending.values()) GL20.glDeleteProgram(p.program);
        for (int prog : ready.values()) GL20.glDeleteProgram(prog);
        pending.clear();
        ready.clear();
    }
}

package com.aetherium.render.gl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.aetherium.render.backend.RenderBackend;
import com.aetherium.util.AetheriumLog;

/**
 * Disk cache for GL program binaries ({@code GL_ARB_get_program_binary}).
 *
 * <p>Load-time shader compilation is the dominant hitch when a world first
 * renders: a mid-range driver spends 200-400 ms per pipeline. Program binaries
 * are driver-version-specific by contract, so a cache is only correct if it is
 * keyed by everything that changes the generated code — which is why
 * {@link #keyFor} hashes the renderer string and the driver version alongside
 * the sources. A stale key can never be hit, so the worst case is a cache miss,
 * not a broken render.</p>
 *
 * <p>Layout: {@code <root>/<key>.bin} with a small text header (format + length)
 * so a corrupt or truncated file is detected before upload rather than producing
 * a GL error mid-frame. Writes are temp-file + atomic-move; a hard crash during a
 * save leaves the previous entry intact.</p>
 */
public final class GlProgramCache implements AutoCloseable {
    private static final AetheriumLog LOGGER = AetheriumLog.of(GlProgramCache.class);
    private static final String MAGIC = "AEPBC1";
    private static final int MAX_BINARY_BYTES = 4 * 1024 * 1024;

    private final Path root;
    private final RenderBackend backend;
    private final String contextKey;

    private int hits;
    private int misses;
    private int stores;
    private int rejected;
    private final List<Path> writtenThisSession = new ArrayList<>(8);

    public GlProgramCache(final Path root, final RenderBackend backend) {
        this.root = Objects.requireNonNull(root, "root");
        this.backend = Objects.requireNonNull(backend, "backend");
        try {
            Files.createDirectories(this.root);
        } catch (final IOException error) {
            throw new IllegalStateException("Cannot create program cache directory " + this.root, error);
        }
        this.contextKey = computeContextKey();
        LOGGER.dev("Program cache at {} keyed {}", this.root, this.contextKey);
    }

    /**
     * Everything that invalidates a binary: renderer string, driver version, GLSL
     * version, backend, and the shader sources themselves.
     */
    private String computeContextKey() {
        return digest(GlProcs.renderer() + '|' + GlProcs.versionString() + '|' + GlProcs.glslVersion() + '|' + this.backend.getId());
    }

    public static String digest(final String text) {
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            final byte[] raw = sha.digest(text.getBytes(StandardCharsets.UTF_8));
            final StringBuilder builder = new StringBuilder(raw.length * 2);
            for (int i = 0; i < 16 && i < raw.length; i++) {
                builder.append(String.format("%02x", raw[i]));
            }
            return builder.toString();
        } catch (final NoSuchAlgorithmException error) {
            // SHA-256 is mandated by the JLS for every conformant JVM; reaching
            // here means a stripped runtime, so degrade rather than crash.
            LOGGER.warn("SHA-256 unavailable; falling back to String.hashCode keys", error);
            return Integer.toHexString(text.hashCode());
        }
    }

    public String keyFor(final List<String> shaderSources, final String defines) {
        Objects.requireNonNull(shaderSources, "shaderSources");
        final StringBuilder joined = new StringBuilder(1024);
        joined.append(this.contextKey).append('|').append(Objects.requireNonNullElse(defines, "")).append('|');
        for (final String source : shaderSources) {
            joined.append(source == null ? "" : source).append('\n');
        }
        return digest(joined.toString());
    }

    public Path fileFor(final String key) {
        return this.root.resolve(key + ".bin");
    }

    /**
     * Loads a cached binary and uploads it.
     *
     * @return true when the program now holds a valid binary and the caller may
     *         skip compilation entirely
     */
    public boolean tryLoad(final int program, final String key) {
        final Path file = fileFor(key);
        if (!Files.isReadable(file)) {
            this.misses++;
            return false;
        }
        try {
            final byte[] raw = Files.readAllBytes(file);
            if (raw.length < MAGIC.length() + 12 || raw.length > MAX_BINARY_BYTES) {
                this.rejected++;
                LOGGER.dev("Rejecting cache entry {} (size {})", file.getFileName(), raw.length);
                return false;
            }
            final ByteBuffer buffer = ByteBuffer.wrap(raw);
            final byte[] magic = new byte[MAGIC.length()];
            buffer.get(magic);
            if (!MAGIC.equals(new String(magic, StandardCharsets.US_ASCII))) {
                this.rejected++;
                return false;
            }
            final int format = buffer.getInt();
            final int length = buffer.getInt();
            if (length <= 0 || length > raw.length - buffer.position()) {
                this.rejected++;
                LOGGER.dev("Rejecting truncated cache entry {} (payload {} > available {})", file.getFileName(), length, buffer.remaining());
                return false;
            }
            final ByteBuffer payload = buffer.slice();
            payload.limit(length);
            if (!GlProcs.programBinary(program, format, payload)) {
                this.rejected++;
                // Driver rejected the binary: it is stale in a way the key missed
                // (e.g. driver updated in place). Drop it so it never costs again.
                deleteQuietly(file);
                return false;
            }
            this.hits++;
            return true;
        } catch (final IOException | RuntimeException error) {
            this.rejected++;
            LOGGER.warn("Ignoring unreadable program cache entry " + file, error);
            return false;
        }
    }

    /** Extracts and stores the binary; a failure here is purely a lost optimization. */
    public void store(final int program, final String key) {
        try {
            final int length = GlProcs.programBinaryLength(program);
            if (length <= 0 || length > MAX_BINARY_BYTES) {
                return;
            }
            final IntBuffer lengthOut = IntBuffer.allocate(1);
            final IntBuffer formatOut = IntBuffer.allocate(1);
            final IntBuffers buffers = new IntBuffers();
            final ByteBuffer binary = org.lwjgl.system.MemoryUtil.memAlloc(length);
            try {
                GlProcs.getProgramBinary(program, binary, buffers.length, buffers.format);
                final int payloadLength = buffers.length.get(0);
                if (payloadLength <= 0 || payloadLength > length) {
                    return;
                }
                final ByteBuffer out = ByteBuffer.allocate(MAGIC.length() + 8 + payloadLength);
                out.put(MAGIC.getBytes(StandardCharsets.US_ASCII));
                out.putInt(buffers.format.get(0));
                out.putInt(payloadLength);
                binary.limit(payloadLength);
                out.put(binary);
                out.flip();
                writeAtomically(out, key);
                this.stores++;
            } finally {
                org.lwjgl.system.MemoryUtil.memFree(binary);
            }
        } catch (final IOException | RuntimeException error) {
            LOGGER.dev("Could not cache program binary for {}: {}", key, error.getClass().getSimpleName());
        }
    }

    private void writeAtomically(final ByteBuffer data, final String key) throws IOException {
        final Path target = fileFor(key);
        final Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        final byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        Files.write(temp, bytes);
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AtomicMoveNotSupportedException unsupported) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        synchronized (this.writtenThisSession) {
            this.writtenThisSession.add(target);
        }
    }

    /** Drops every entry (GUI button "Clear shader cache"), keyed on backend. */
    public int clearAll() {
        int removed = 0;
        try (java.util.stream.Stream<Path> files = Files.list(this.root)) {
            final List<Path> targets = new ArrayList<>();
            files.filter(path -> path.getFileName().toString().endsWith(".bin")).forEach(targets::add);
            for (final Path path : targets) {
                if (deleteQuietly(path)) {
                    removed++;
                }
            }
        } catch (final IOException error) {
            LOGGER.warn("Could not enumerate the program cache directory " + this.root, error);
        }
        LOGGER.info("Cleared {} program cache entries", removed);
        return removed;
    }

    private static boolean deleteQuietly(final Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (final IOException error) {
            LOGGER.dev("Could not delete {}: {}", path, error.getMessage());
            return false;
        }
    }

    public List<Path> getWrittenThisSession() {
        synchronized (this.writtenThisSession) {
            return Collections.unmodifiableList(new ArrayList<>(this.writtenThisSession));
        }
    }

    public String describe() {
        final int total = this.hits + this.misses;
        return String.format(Locale.ROOT, "program cache: %d hits / %d lookups, %d stored, %d rejected",
                this.hits, total == 0 ? 1 : total, this.stores, this.rejected);
    }

    public int getHits() {
        return this.hits;
    }

    public int getMisses() {
        return this.misses;
    }

    public int getStores() {
        return this.stores;
    }

    public int getRejected() {
        return this.rejected;
    }

    @Override
    public void close() {
        LOGGER.info("{} (backend {})", describe(), this.backend.getId());
    }

    /** Small holder so glGetProgramBinary's out-params stay allocation-free. */
    private static final class IntBuffers {
        private final java.nio.IntBuffer length = java.nio.IntBuffer.allocate(1);
        private final java.nio.IntBuffer format = java.nio.IntBuffer.allocate(1);
    }
}

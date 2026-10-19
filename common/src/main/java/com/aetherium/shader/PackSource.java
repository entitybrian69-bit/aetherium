package com.aetherium.shader;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Read-only access to a shader pack's {@code shaders/} folder, from a directory, a zip file or
 * the jar's bundled packs. Paths are relative to the {@code shaders/} folder and use forward
 * slashes ({@code "lib/settings.glsl"}, {@code "world-1/composite.fsh"}).
 *
 * <p>Zips are read fully into memory once (packs are a few MB at most): the files are read many
 * times while options, includes and programs are resolved, and the zip handle is closed at once.</p>
 */
public abstract class PackSource implements Closeable {
    /** File contents, or null when missing. */
    public abstract byte[] bytes(String path);

    /** Every file path in the pack (relative to {@code shaders/}). */
    public abstract List<String> files();

    public boolean exists(final String path) {
        return this.bytes(path) != null;
    }

    /** UTF-8 text with Windows line endings removed, or null when missing. */
    public String text(final String path) {
        final byte[] b = this.bytes(normalize(path));
        if (b == null) {
            return null;
        }
        String s = new String(b, StandardCharsets.UTF_8);
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1);
        }
        return s.indexOf('\r') >= 0 ? s.replace("\r\n", "\n").replace('\r', '\n') : s;
    }

    /** True when the folder {@code dir} (no trailing slash) holds at least one file. */
    public boolean hasDir(final String dir) {
        final String prefix = dir + "/";
        for (final String f : this.files()) {
            if (f.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
    }

    /** Resolves "a/b/../c" and "./x", drops a leading slash. */
    public static String normalize(final String path) {
        final String[] parts = path.replace('\\', '/').split("/");
        final List<String> out = new ArrayList<>();
        for (final String p : parts) {
            if (p.isEmpty() || ".".equals(p)) {
                continue;
            }
            if ("..".equals(p)) {
                if (!out.isEmpty()) {
                    out.remove(out.size() - 1);
                }
                continue;
            }
            out.add(p);
        }
        return String.join("/", out);
    }

    /** Opens a pack folder or zip. Returns null when it has no {@code shaders/} folder. */
    public static PackSource open(final Path path) throws IOException {
        if (Files.isDirectory(path)) {
            final Path shaders = path.resolve("shaders");
            return Files.isDirectory(shaders) ? new Dir(shaders) : null;
        }
        if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".zip")) {
            final Map<String, byte[]> all = new HashMap<>();
            try (ZipFile zip = new ZipFile(path.toFile())) {
                final Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) {
                    final ZipEntry e = en.nextElement();
                    if (e.isDirectory() || e.getSize() > 64L << 20) {
                        continue;
                    }
                    try (InputStream in = zip.getInputStream(e)) {
                        all.put(e.getName().replace('\\', '/'), readAll(in));
                    }
                }
            }
            return Memory.fromZip(all);
        }
        return null;
    }

    static byte[] readAll(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, in.available()));
        final byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /** A pack held in memory: zip packs, the bundled lite pack and unit tests. */
    public static final class Memory extends PackSource {
        private final Map<String, byte[]> files;
        private final List<String> names;

        public Memory(final Map<String, byte[]> files) {
            this.files = files;
            final List<String> n = new ArrayList<>(files.keySet());
            Collections.sort(n);
            this.names = Collections.unmodifiableList(n);
        }

        /**
         * Picks the {@code shaders/} folder of a zip: at the root, or one folder down (packs zipped
         * with their top folder). Returns null when there is none.
         */
        static Memory fromZip(final Map<String, byte[]> all) {
            String root = null;
            for (final String name : all.keySet()) {
                final int at = name.startsWith("shaders/") ? 0 : name.indexOf("/shaders/");
                if (at < 0) {
                    continue;
                }
                final String candidate = at == 0 ? "shaders/" : name.substring(0, at) + "/shaders/";
                if (root == null || candidate.length() < root.length()) {
                    root = candidate;
                }
            }
            if (root == null) {
                return null;
            }
            final Map<String, byte[]> inside = new HashMap<>();
            for (final Map.Entry<String, byte[]> e : all.entrySet()) {
                if (e.getKey().startsWith(root)) {
                    inside.put(e.getKey().substring(root.length()), e.getValue());
                }
            }
            return new Memory(inside);
        }

        public static Memory ofText(final Map<String, String> text) {
            final Map<String, byte[]> m = new HashMap<>();
            for (final Map.Entry<String, String> e : text.entrySet()) {
                m.put(e.getKey(), e.getValue().getBytes(StandardCharsets.UTF_8));
            }
            return new Memory(m);
        }

        @Override
        public byte[] bytes(final String path) {
            return this.files.get(normalize(path));
        }

        @Override
        public List<String> files() {
            return this.names;
        }
    }

    private static final class Dir extends PackSource {
        private final Path root;
        private List<String> names;

        Dir(final Path root) {
            this.root = root;
        }

        @Override
        public byte[] bytes(final String path) {
            final String rel = normalize(path);
            if (rel.isEmpty()) {
                return null;
            }
            final Path p = this.root.resolve(rel);
            try {
                return Files.isRegularFile(p) ? Files.readAllBytes(p) : null;
            } catch (final IOException e) {
                return null;
            }
        }

        @Override
        public List<String> files() {
            if (this.names == null) {
                final List<String> out = new ArrayList<>();
                try (java.util.stream.Stream<Path> walk = Files.walk(this.root, 12)) {
                    walk.filter(Files::isRegularFile)
                            .forEach(p -> out.add(this.root.relativize(p).toString().replace('\\', '/')));
                } catch (final IOException | RuntimeException ignored) {
                    // unreadable folders: list what was found
                }
                Collections.sort(out);
                this.names = Collections.unmodifiableList(out);
            }
            return this.names;
        }
    }
}

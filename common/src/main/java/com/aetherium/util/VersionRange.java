package com.aetherium.util;

import java.util.Objects;

/**
 * Minecraft-shaped version comparison: componentwise, so {@code 1.21.10 > 1.21.9}, and
 * interval syntax borrowed from Gradle ({@code [1.20.2,)}, {@code [1.16.5,1.17)}).
 *
 * <p>This lives in {@code util} rather than inside
 * {@link com.aetherium.mixin.AetheriumMixinPlugin} for two reasons. The first is
 * testability: the plugin implements a Mixin interface and cannot be loaded in a plain
 * unit-test JVM, but a wrong range is exactly the kind of bug that ships a version with
 * one feature silently off. The second is that the range strings are written by
 * {@code tools/gen_deltas.py}, so the parser is part of the porting contract and must be
 * readable without opening a mixin file.</p>
 *
 * <p>{@link #compare} treats a missing component as zero (so {@code 1.21} equals
 * {@code 1.21.0}) and a non-numeric suffix as a tie-breaker that sorts a prerelease
 * before the release it previews, which is how {@code 1.20.2-rc1 < 1.20.2} behaves.
 * Date-style ids ({@code 26.3}) need no special case: the leading component is simply
 * larger, which is why {@code [1.16.5,)} covers them too.</p>
 */
public final class VersionRange {
    /** Anything at or above the reference version; used when a mixin is universal. */
    public static final String ANY = "*";

    private VersionRange() {
    }

    /**
     * @param version a Minecraft version string, e.g. {@code "1.21.1"} or {@code "26.3"}
     * @param range   {@link #ANY}, or an interval {@code "[lo,hi)"}/{@code "[lo,hi]"} with
     *                either bound empty ({@code "[1.20.2,)"})
     * @return true when {@code version} is inside {@code range}; false for a malformed
     *         range, because a range we cannot read must not authorize a transform
     */
    public static boolean contains(final String version, final String range) {
        Objects.requireNonNull(version, "version");
        if (range == null || range.isEmpty() || ANY.equals(range)) {
            return true;
        }
        return inRange(version, range.trim());
    }

    /** True when the range string is syntactically usable. Used by tools/verify.sh. */
    public static boolean isWellFormed(final String range) {
        if (range == null || ANY.equals(range)) {
            return true;
        }
        final String text = range.trim();
        if (text.length() < 4) {
            return false;
        }
        final char open = text.charAt(0);
        final char close = text.charAt(text.length() - 1);
        if ((open != '[' && open != '(') || (close != ']' && close != ')')) {
            return false;
        }
        final int comma = text.indexOf(',');
        if (comma < 0) {
            return false;
        }
        final String lower = text.substring(1, comma).trim();
        final String upper = text.substring(comma + 1, text.length() - 1).trim();
        return (lower.isEmpty() || startsWithDigit(lower)) && (upper.isEmpty() || startsWithDigit(upper));
    }

    private static boolean startsWithDigit(final String text) {
        return !text.isEmpty() && Character.isDigit(text.charAt(0));
    }

    private static boolean inRange(final String version, final String range) {
        final int comma = range.indexOf(',');
        if (comma < 0) {
            return false;
        }
        final String lower = range.substring(1, comma).trim();
        final String upper = range.substring(comma + 1, range.length() - 1).trim();
        if (!lower.isEmpty() && compare(version, lower) < 0) {
            return false;
        }
        if (!upper.isEmpty()) {
            final int cmp = compare(version, upper);
            final boolean exclusiveEnd = range.endsWith(")");
            return exclusiveEnd ? cmp < 0 : cmp <= 0;
        }
        return true;
    }

    /**
     * Componentwise comparison. Numeric parts compare as numbers (so {@code 10 > 9});
     * the remainder of a component, if any, compares lexically.
     */
    public static int compare(final String left, final String right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        final String[] a = left.split("[.\\-+]");
        final String[] b = right.split("[.\\-+]");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            final String partA = i < a.length ? a[i] : "0";
            final String partB = i < b.length ? b[i] : "0";
            final long numA = leadingNumber(partA);
            final long numB = leadingNumber(partB);
            if (numA != numB) {
                return numA < numB ? -1 : 1;
            }
            final String restA = trailingText(partA);
            final String restB = trailingText(partB);
            if (!restA.equals(restB)) {
                // A component with no suffix is the release, and a release outranks its
                // own prereleases: "" > "rc1" would be wrong, so the empty string sorts
                // last by comparing reversed.
                if (restA.isEmpty()) {
                    return 1;
                }
                if (restB.isEmpty()) {
                    return -1;
                }
                return restA.compareTo(restB);
            }
        }
        return 0;
    }

    private static long leadingNumber(final String part) {
        long value = 0L;
        for (int i = 0; i < part.length(); i++) {
            final char c = part.charAt(i);
            if (c < '0' || c > '9') {
                return i == 0 ? -1L : value;
            }
            value = value * 10L + (c - '0');
        }
        return value;
    }

    private static String trailingText(final String part) {
        int i = 0;
        while (i < part.length() && Character.isDigit(part.charAt(i))) {
            i++;
        }
        return part.substring(i);
    }
}

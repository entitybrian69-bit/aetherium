package com.aetherium.shader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small C preprocessor for reading shader packs: object-like {@code #define}/{@code #undef},
 * {@code #if/#ifdef/#ifndef/#elif/#else/#endif} with full integer/float expressions and
 * {@code defined}. It answers "which lines are active with these macros", which the engine needs
 * for {@code shaders.properties}, {@code block.properties}, {@code const} settings and the
 * {@code DRAWBUFFERS}/{@code RENDERTARGETS} comments. The GLSL itself is still preprocessed by the
 * driver; this never rewrites shader code.
 *
 * <p>Function-like macros are recorded as defined (for {@code #ifdef}) but not expanded.</p>
 */
public final class Preprocessor {
    private final Map<String, String> macros;

    public Preprocessor(final Map<String, String> initial) {
        this.macros = new HashMap<>(initial);
    }

    public Map<String, String> macros() {
        return this.macros;
    }

    /** The lines that are active, with directive lines removed and defines applied in order. */
    public List<String> activeLines(final String text) {
        final List<String> out = new ArrayList<>();
        final String[] lines = joinContinuations(text).split("\n", -1);
        // Each frame: [active, anyBranchTaken, parentActive]
        final List<boolean[]> stack = new ArrayList<>();
        boolean active = true;
        for (final String raw : lines) {
            final String line = raw.trim();
            if (line.startsWith("#")) {
                final String d = line.substring(1).trim();
                final String word = firstWord(d);
                final String rest = d.substring(word.length()).trim();
                switch (word) {
                    case "ifdef":
                    case "ifndef":
                    case "if": {
                        boolean cond = false;
                        if (active) {
                            if ("if".equals(word)) {
                                cond = this.eval(rest) != 0;
                            } else {
                                final boolean def = this.macros.containsKey(firstWord(stripComment(rest)));
                                cond = "ifdef".equals(word) == def;
                            }
                        }
                        stack.add(new boolean[] {active && cond, active && cond, active});
                        active = active && cond;
                        continue;
                    }
                    case "elif": {
                        if (stack.isEmpty()) {
                            continue;
                        }
                        final boolean[] f = stack.get(stack.size() - 1);
                        if (f[2] && !f[1] && this.eval(rest) != 0) {
                            f[0] = true;
                            f[1] = true;
                        } else {
                            f[0] = false;
                        }
                        active = f[0];
                        continue;
                    }
                    case "else": {
                        if (stack.isEmpty()) {
                            continue;
                        }
                        final boolean[] f = stack.get(stack.size() - 1);
                        f[0] = f[2] && !f[1];
                        f[1] = true;
                        active = f[0];
                        continue;
                    }
                    case "endif": {
                        if (!stack.isEmpty()) {
                            final boolean[] f = stack.remove(stack.size() - 1);
                            active = f[2];
                        }
                        continue;
                    }
                    case "define":
                        if (active) {
                            this.define(rest);
                        }
                        continue;
                    case "undef":
                        if (active) {
                            this.macros.remove(firstWord(rest));
                        }
                        continue;
                    default:
                        if (active) {
                            out.add(raw); // #version, #extension, #include (already expanded), #line ...
                        }
                        continue;
                }
            }
            if (active) {
                out.add(raw);
            }
        }
        return out;
    }

    private void define(final String rest) {
        final String body = stripComment(rest);
        int i = 0;
        while (i < body.length() && isIdent(body.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return;
        }
        final String name = body.substring(0, i);
        if (i < body.length() && body.charAt(i) == '(') {
            this.macros.put(name, "1"); // function-like: defined, not expanded
            return;
        }
        this.macros.put(name, body.substring(i).trim());
    }

    static String joinContinuations(final String text) {
        return text.indexOf("\\\n") >= 0 ? text.replace("\\\n", " ") : text;
    }

    static String firstWord(final String s) {
        int i = 0;
        while (i < s.length() && isIdent(s.charAt(i))) {
            i++;
        }
        return s.substring(0, i);
    }

    static boolean isIdent(final char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    /** Removes a trailing {@code //} or {@code /* *}{@code /} comment. */
    static String stripComment(final String s) {
        final int line = s.indexOf("//");
        final int block = s.indexOf("/*");
        int cut = s.length();
        if (line >= 0) {
            cut = line;
        }
        if (block >= 0 && block < cut) {
            cut = block;
        }
        return s.substring(0, cut).trim();
    }

    // ------------------------------------------------------------------ expressions

    /** Evaluates a {@code #if} expression; unknown identifiers are 0, errors are 0. */
    public double eval(final String expr) {
        try {
            final Parser p = new Parser(this.expand(stripComment(expr), 0));
            final double v = p.ternary();
            return Double.isNaN(v) ? 0 : v;
        } catch (final RuntimeException e) {
            return 0;
        }
    }

    /** Expands object-like macros (keeps the operand of {@code defined}). */
    private String expand(final String s, final int depth) {
        if (depth > 16) {
            return s;
        }
        final StringBuilder out = new StringBuilder(s.length() + 16);
        boolean changed = false;
        boolean afterDefined = false;
        int i = 0;
        while (i < s.length()) {
            final char c = s.charAt(i);
            if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < s.length() && isIdent(s.charAt(j))) {
                    j++;
                }
                final String id = s.substring(i, j);
                if ("defined".equals(id)) {
                    afterDefined = true;
                    out.append(id);
                } else if (afterDefined) {
                    afterDefined = false;
                    out.append(id);
                } else {
                    final String v = this.macros.get(id);
                    if (v != null && !v.isEmpty() && !v.equals(id)) {
                        out.append('(').append(v).append(')');
                        changed = true;
                    } else {
                        out.append(id);
                    }
                }
                i = j;
                continue;
            }
            if (c != '(' && c != ' ' && c != '\t') {
                afterDefined = false;
            }
            out.append(c);
            i++;
        }
        return changed ? this.expand(out.toString(), depth + 1) : out.toString();
    }

    private final class Parser {
        private final String s;
        private int pos;

        Parser(final String s) {
            this.s = s;
        }

        private void ws() {
            while (this.pos < this.s.length() && Character.isWhitespace(this.s.charAt(this.pos))) {
                this.pos++;
            }
        }

        private boolean eat(final String op) {
            this.ws();
            if (this.s.startsWith(op, this.pos)) {
                // do not split "&&" into "&" or "<=" into "<"
                final int end = this.pos + op.length();
                if (op.length() == 1 && end < this.s.length()) {
                    final char n = this.s.charAt(end);
                    final char c = op.charAt(0);
                    if ((c == '&' && n == '&') || (c == '|' && n == '|') || ((c == '<' || c == '>') && (n == '=' || n == c))
                            || ((c == '=' || c == '!') && n == '=')) {
                        return false;
                    }
                }
                if (op.length() == 2 && end < this.s.length() && (op.equals("<<") || op.equals(">>"))
                        && this.s.charAt(end) == '=') {
                    return false;
                }
                this.pos = end;
                return true;
            }
            return false;
        }

        double ternary() {
            final double c = this.or();
            if (this.eat("?")) {
                final double a = this.ternary();
                this.eat(":");
                final double b = this.ternary();
                return c != 0 ? a : b;
            }
            return c;
        }

        private double or() {
            double v = this.and();
            while (this.eat("||")) {
                final double r = this.and();
                v = (v != 0 || r != 0) ? 1 : 0;
            }
            return v;
        }

        private double and() {
            double v = this.bitOr();
            while (this.eat("&&")) {
                final double r = this.bitOr();
                v = (v != 0 && r != 0) ? 1 : 0;
            }
            return v;
        }

        private double bitOr() {
            double v = this.bitXor();
            while (this.eat("|")) {
                v = (long) v | (long) this.bitXor();
            }
            return v;
        }

        private double bitXor() {
            double v = this.bitAnd();
            while (this.eat("^")) {
                v = (long) v ^ (long) this.bitAnd();
            }
            return v;
        }

        private double bitAnd() {
            double v = this.equality();
            while (this.eat("&")) {
                v = (long) v & (long) this.equality();
            }
            return v;
        }

        private double equality() {
            double v = this.relational();
            while (true) {
                if (this.eat("==")) {
                    v = v == this.relational() ? 1 : 0;
                } else if (this.eat("!=")) {
                    v = v != this.relational() ? 1 : 0;
                } else {
                    return v;
                }
            }
        }

        private double relational() {
            double v = this.shift();
            while (true) {
                if (this.eat("<=")) {
                    v = v <= this.shift() ? 1 : 0;
                } else if (this.eat(">=")) {
                    v = v >= this.shift() ? 1 : 0;
                } else if (this.eat("<")) {
                    v = v < this.shift() ? 1 : 0;
                } else if (this.eat(">")) {
                    v = v > this.shift() ? 1 : 0;
                } else {
                    return v;
                }
            }
        }

        private double shift() {
            double v = this.additive();
            while (true) {
                if (this.eat("<<")) {
                    v = (long) v << (long) this.additive();
                } else if (this.eat(">>")) {
                    v = (long) v >> (long) this.additive();
                } else {
                    return v;
                }
            }
        }

        private double additive() {
            double v = this.multiplicative();
            while (true) {
                if (this.eat("+")) {
                    v += this.multiplicative();
                } else if (this.eat("-")) {
                    v -= this.multiplicative();
                } else {
                    return v;
                }
            }
        }

        private double multiplicative() {
            double v = this.unary();
            while (true) {
                if (this.eat("*")) {
                    v *= this.unary();
                } else if (this.eat("/")) {
                    final double d = this.unary();
                    v = d == 0 ? 0 : (isInt(v) && isInt(d) ? (long) v / (long) d : v / d);
                } else if (this.eat("%")) {
                    final double d = this.unary();
                    v = d == 0 ? 0 : v % d;
                } else {
                    return v;
                }
            }
        }

        private double unary() {
            if (this.eat("!")) {
                return this.unary() == 0 ? 1 : 0;
            }
            if (this.eat("-")) {
                return -this.unary();
            }
            if (this.eat("+")) {
                return this.unary();
            }
            if (this.eat("~")) {
                return ~(long) this.unary();
            }
            return this.primary();
        }

        private double primary() {
            this.ws();
            if (this.eat("(")) {
                final double v = this.ternary();
                this.eat(")");
                return v;
            }
            if (this.pos >= this.s.length()) {
                return 0;
            }
            final char c = this.s.charAt(this.pos);
            if (Character.isDigit(c) || c == '.') {
                int j = this.pos;
                while (j < this.s.length() && (Character.isLetterOrDigit(this.s.charAt(j)) || this.s.charAt(j) == '.'
                        || ((this.s.charAt(j) == '-' || this.s.charAt(j) == '+') && j > this.pos
                        && (this.s.charAt(j - 1) == 'e' || this.s.charAt(j - 1) == 'E')
                        && !this.s.startsWith("0x", this.pos)))) {
                    j++;
                }
                final String num = this.s.substring(this.pos, j);
                this.pos = j;
                return parseNumber(num);
            }
            if (Character.isLetter(c) || c == '_') {
                int j = this.pos;
                while (j < this.s.length() && isIdent(this.s.charAt(j))) {
                    j++;
                }
                final String id = this.s.substring(this.pos, j);
                this.pos = j;
                if ("defined".equals(id)) {
                    final boolean paren = this.eat("(");
                    this.ws();
                    int k = this.pos;
                    while (k < this.s.length() && isIdent(this.s.charAt(k))) {
                        k++;
                    }
                    final String name = this.s.substring(this.pos, k);
                    this.pos = k;
                    if (paren) {
                        this.eat(")");
                    }
                    return Preprocessor.this.macros.containsKey(name) ? 1 : 0;
                }
                if ("true".equals(id)) {
                    return 1;
                }
                if (this.eat("(")) { // unknown function-like macro call: skip its arguments
                    int depth = 1;
                    while (this.pos < this.s.length() && depth > 0) {
                        final char ch = this.s.charAt(this.pos++);
                        if (ch == '(') {
                            depth++;
                        } else if (ch == ')') {
                            depth--;
                        }
                    }
                }
                return 0;
            }
            this.pos++;
            return 0;
        }
    }

    static boolean isInt(final double v) {
        return v == Math.rint(v) && !Double.isInfinite(v);
    }

    /** Parses 12, 0x1F, 1.5, 2.0f, 1e3, 10u, 5L; returns NaN when it is not a number. */
    public static double parseNumber(final String raw) {
        String n = raw.trim();
        if (n.isEmpty()) {
            return Double.NaN;
        }
        try {
            if (n.startsWith("0x") || n.startsWith("0X")) {
                return Long.parseLong(n.substring(2).replaceAll("[uUlL]+$", ""), 16);
            }
            while (!n.isEmpty() && "fFuUlLdD".indexOf(n.charAt(n.length() - 1)) >= 0
                    && !(n.length() > 1 && (n.charAt(n.length() - 2) == 'e' || n.charAt(n.length() - 2) == 'E'))) {
                n = n.substring(0, n.length() - 1);
            }
            return Double.parseDouble(n);
        } catch (final NumberFormatException e) {
            return Double.NaN;
        }
    }
}

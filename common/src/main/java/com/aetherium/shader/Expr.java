package com.aetherium.shader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * OptiFine's custom uniform expression language ({@code uniform.<type>.<name>=<expr>} and
 * {@code variable.*} in {@code shaders.properties}, {@code program.X.enabled=<expr>}).
 *
 * <p>Expressions compile once into a node tree; {@link #eval} walks it with an {@link Env} that
 * resolves parameters (worldTime, sunPosition.y, biome, is_hurt, ...). Every value is a
 * {@code double[]} of 1 to 4 components; booleans are 1/0. Evaluated once per frame per custom
 * uniform, never per draw.</p>
 */
public abstract class Expr {
    /** Resolves names at evaluation time. */
    public interface Env {
        /** A scalar parameter or vector component ("sunPosition.y"); NaN when unknown. */
        double scalar(String name);

        /** A whole vector parameter ("sunPosition"); null when unknown or scalar. */
        double[] vector(String name);

        /** Seconds since start, for smooth(). */
        double time();
    }

    public abstract double[] eval(Env env);

    public double evalScalar(final Env env) {
        final double[] v = this.eval(env);
        return v.length == 0 ? 0 : v[0];
    }

    // ------------------------------------------------------------------ constants

    private static final Map<String, Double> CONSTANTS = new HashMap<>();
    private static final Random RANDOM = new Random();

    static {
        CONSTANTS.put("pi", Math.PI);
        CONSTANTS.put("true", 1.0);
        CONSTANTS.put("false", 0.0);
        final String[] cats = {"NONE", "TAIGA", "EXTREME_HILLS", "JUNGLE", "MESA", "PLAINS", "SAVANNA", "ICY",
                "THE_END", "BEACH", "FOREST", "OCEAN", "DESERT", "RIVER", "SWAMP", "MUSHROOM", "NETHER"};
        for (int i = 0; i < cats.length; i++) {
            CONSTANTS.put("CAT_" + cats[i], (double) i);
        }
        CONSTANTS.put("PPT_NONE", 0.0);
        CONSTANTS.put("PPT_RAIN", 1.0);
        CONSTANTS.put("PPT_SNOW", 2.0);
        // Vanilla 1.16.5 numeric biome ids (the ids OptiFine exposes as BIOME_<NAME>)
        final String[] biomes = {"OCEAN", "PLAINS", "DESERT", "MOUNTAINS", "FOREST", "TAIGA", "SWAMP", "RIVER",
                "NETHER_WASTES", "THE_END", "FROZEN_OCEAN", "FROZEN_RIVER", "SNOWY_TUNDRA", "SNOWY_MOUNTAINS",
                "MUSHROOM_FIELDS", "MUSHROOM_FIELD_SHORE", "BEACH", "DESERT_HILLS", "WOODED_HILLS", "TAIGA_HILLS",
                "MOUNTAIN_EDGE", "JUNGLE", "JUNGLE_HILLS", "JUNGLE_EDGE", "DEEP_OCEAN", "STONE_SHORE", "SNOWY_BEACH",
                "BIRCH_FOREST", "BIRCH_FOREST_HILLS", "DARK_FOREST", "SNOWY_TAIGA", "SNOWY_TAIGA_HILLS",
                "GIANT_TREE_TAIGA", "GIANT_TREE_TAIGA_HILLS", "WOODED_MOUNTAINS", "SAVANNA", "SAVANNA_PLATEAU",
                "BADLANDS", "WOODED_BADLANDS_PLATEAU", "BADLANDS_PLATEAU", "SMALL_END_ISLANDS", "END_MIDLANDS",
                "END_HIGHLANDS", "END_BARRENS", "WARM_OCEAN", "LUKEWARM_OCEAN", "COLD_OCEAN", "DEEP_WARM_OCEAN",
                "DEEP_LUKEWARM_OCEAN", "DEEP_COLD_OCEAN", "DEEP_FROZEN_OCEAN"};
        for (int i = 0; i < biomes.length; i++) {
            CONSTANTS.put("BIOME_" + biomes[i], (double) i);
        }
        final Object[][] more = {{"THE_VOID", 127}, {"SUNFLOWER_PLAINS", 129}, {"DESERT_LAKES", 130},
                {"GRAVELLY_MOUNTAINS", 131}, {"FLOWER_FOREST", 132}, {"TAIGA_MOUNTAINS", 133}, {"SWAMP_HILLS", 134},
                {"ICE_SPIKES", 140}, {"MODIFIED_JUNGLE", 149}, {"MODIFIED_JUNGLE_EDGE", 151},
                {"TALL_BIRCH_FOREST", 155}, {"TALL_BIRCH_HILLS", 156}, {"DARK_FOREST_HILLS", 157},
                {"SNOWY_TAIGA_MOUNTAINS", 158}, {"GIANT_SPRUCE_TAIGA", 160}, {"GIANT_SPRUCE_TAIGA_HILLS", 161},
                {"MODIFIED_GRAVELLY_MOUNTAINS", 162}, {"SHATTERED_SAVANNA", 163},
                {"SHATTERED_SAVANNA_PLATEAU", 164}, {"ERODED_BADLANDS", 165},
                {"MODIFIED_WOODED_BADLANDS_PLATEAU", 166}, {"MODIFIED_BADLANDS_PLATEAU", 167},
                {"BAMBOO_JUNGLE", 168}, {"BAMBOO_JUNGLE_HILLS", 169}, {"SOUL_SAND_VALLEY", 170},
                {"CRIMSON_FOREST", 171}, {"WARPED_FOREST", 172}, {"BASALT_DELTAS", 173}};
        for (final Object[] m : more) {
            CONSTANTS.put("BIOME_" + m[0], ((Integer) m[1]).doubleValue());
        }
    }

    /** OptiFine's numeric id of a 1.16.5 biome path ("dark_forest" -> 29), or -1. */
    public static int biomeId(final String path) {
        final Double v = CONSTANTS.get("BIOME_" + path.toUpperCase(java.util.Locale.ROOT));
        return v == null ? -1 : v.intValue();
    }

    // ------------------------------------------------------------------ parsing

    /** Parses an expression; throws IllegalArgumentException with a readable message on errors. */
    public static Expr parse(final String text) {
        final P p = new P(text);
        final Expr e = p.or();
        p.ws();
        if (p.pos != p.s.length()) {
            throw new IllegalArgumentException("unexpected '" + p.s.substring(p.pos) + "' in " + text);
        }
        return e;
    }

    private static final class P {
        final String s;
        int pos;

        P(final String s) {
            this.s = s;
        }

        void ws() {
            while (this.pos < this.s.length() && Character.isWhitespace(this.s.charAt(this.pos))) {
                this.pos++;
            }
        }

        boolean eat(final String op) {
            this.ws();
            if (!this.s.startsWith(op, this.pos)) {
                return false;
            }
            final int end = this.pos + op.length();
            if (op.length() == 1 && end < this.s.length()) {
                final char n = this.s.charAt(end);
                final char c = op.charAt(0);
                if ((c == '<' || c == '>' || c == '!' || c == '=') && n == '=') {
                    return false;
                }
                if ((c == '&' && n == '&') || (c == '|' && n == '|')) {
                    return false;
                }
            }
            this.pos = end;
            return true;
        }

        Expr or() {
            Expr e = this.and();
            while (this.eat("||")) {
                e = bin(e, this.and(), 'o');
            }
            return e;
        }

        Expr and() {
            Expr e = this.equality();
            while (this.eat("&&")) {
                e = bin(e, this.equality(), 'a');
            }
            return e;
        }

        Expr equality() {
            Expr e = this.rel();
            while (true) {
                if (this.eat("==")) {
                    e = bin(e, this.rel(), '=');
                } else if (this.eat("!=")) {
                    e = bin(e, this.rel(), '!');
                } else {
                    return e;
                }
            }
        }

        Expr rel() {
            Expr e = this.add();
            while (true) {
                if (this.eat(">=")) {
                    e = bin(e, this.add(), 'G');
                } else if (this.eat("<=")) {
                    e = bin(e, this.add(), 'L');
                } else if (this.eat(">")) {
                    e = bin(e, this.add(), '>');
                } else if (this.eat("<")) {
                    e = bin(e, this.add(), '<');
                } else {
                    return e;
                }
            }
        }

        Expr add() {
            Expr e = this.mul();
            while (true) {
                if (this.eat("+")) {
                    e = bin(e, this.mul(), '+');
                } else if (this.eat("-")) {
                    e = bin(e, this.mul(), '-');
                } else {
                    return e;
                }
            }
        }

        Expr mul() {
            Expr e = this.unary();
            while (true) {
                if (this.eat("*")) {
                    e = bin(e, this.unary(), '*');
                } else if (this.eat("/")) {
                    e = bin(e, this.unary(), '/');
                } else if (this.eat("%")) {
                    e = bin(e, this.unary(), '%');
                } else {
                    return e;
                }
            }
        }

        Expr unary() {
            if (this.eat("!")) {
                final Expr a = this.unary();
                return new Expr() {
                    @Override
                    public double[] eval(final Env env) {
                        return new double[] {a.evalScalar(env) == 0 ? 1 : 0};
                    }
                };
            }
            if (this.eat("-")) {
                final Expr a = this.unary();
                return new Expr() {
                    @Override
                    public double[] eval(final Env env) {
                        final double[] v = a.eval(env).clone();
                        for (int i = 0; i < v.length; i++) {
                            v[i] = -v[i];
                        }
                        return v;
                    }
                };
            }
            if (this.eat("+")) {
                return this.unary();
            }
            return this.primary();
        }

        Expr primary() {
            this.ws();
            if (this.eat("(")) {
                final Expr e = this.or();
                if (!this.eat(")")) {
                    throw new IllegalArgumentException("missing ')' in " + this.s);
                }
                return e;
            }
            if (this.pos >= this.s.length()) {
                throw new IllegalArgumentException("unexpected end of " + this.s);
            }
            final char c = this.s.charAt(this.pos);
            if (Character.isDigit(c) || c == '.') {
                int j = this.pos;
                while (j < this.s.length() && (Character.isDigit(this.s.charAt(j)) || this.s.charAt(j) == '.'
                        || this.s.charAt(j) == 'e' || this.s.charAt(j) == 'E'
                        || ((this.s.charAt(j) == '-' || this.s.charAt(j) == '+')
                        && (this.s.charAt(j - 1) == 'e' || this.s.charAt(j - 1) == 'E')))) {
                    j++;
                }
                if (j < this.s.length() && (this.s.charAt(j) == 'f' || this.s.charAt(j) == 'F')) {
                    j++;
                }
                final double v = Preprocessor.parseNumber(this.s.substring(this.pos, j));
                this.pos = j;
                return constant(v);
            }
            if (Character.isLetter(c) || c == '_') {
                int j = this.pos;
                while (j < this.s.length() && (Preprocessor.isIdent(this.s.charAt(j)) || this.s.charAt(j) == '.')) {
                    j++;
                }
                final String id = this.s.substring(this.pos, j);
                this.pos = j;
                if (this.eat("(")) {
                    final List<Expr> args = new ArrayList<>();
                    if (!this.eat(")")) {
                        do {
                            args.add(this.or());
                        } while (this.eat(","));
                        if (!this.eat(")")) {
                            throw new IllegalArgumentException("missing ')' after " + id + "( in " + this.s);
                        }
                    }
                    return function(id, args);
                }
                final Double k = CONSTANTS.get(id);
                if (k != null) {
                    return constant(k);
                }
                return variable(id);
            }
            throw new IllegalArgumentException("unexpected '" + c + "' in " + this.s);
        }
    }

    static Expr constant(final double v) {
        return new Literal(v);
    }

    /** A number literal or named constant. */
    static final class Literal extends Expr {
        private final double[] value;

        Literal(final double v) {
            this.value = new double[] {v};
        }

        @Override
        public double[] eval(final Env env) {
            return this.value;
        }
    }

    private static final double[] ZERO = {0};

    static Expr variable(final String name) {
        return new Expr() {
            @Override
            public double[] eval(final Env env) {
                final double s = env.scalar(name);
                if (!Double.isNaN(s)) {
                    return new double[] {s};
                }
                final double[] v = env.vector(name);
                return v != null ? v : ZERO;
            }
        };
    }

    static Expr bin(final Expr a, final Expr b, final char op) {
        return new Expr() {
            @Override
            public double[] eval(final Env env) {
                final double[] x = a.eval(env);
                final double[] y = b.eval(env);
                final int n = Math.max(x.length, y.length);
                final double[] out = new double[op == '+' || op == '-' || op == '*' || op == '/' || op == '%' ? n : 1];
                for (int i = 0; i < out.length; i++) {
                    final double p = x[Math.min(i, x.length - 1)];
                    final double q = y[Math.min(i, y.length - 1)];
                    out[i] = apply(op, p, q);
                }
                return out;
            }
        };
    }

    static double apply(final char op, final double p, final double q) {
        switch (op) {
            case '+': return p + q;
            case '-': return p - q;
            case '*': return p * q;
            case '/': return q == 0 ? 0 : p / q;
            case '%': return q == 0 ? 0 : p % q;
            case 'o': return p != 0 || q != 0 ? 1 : 0;
            case 'a': return p != 0 && q != 0 ? 1 : 0;
            case '=': return p == q ? 1 : 0;
            case '!': return p != q ? 1 : 0;
            case '>': return p > q ? 1 : 0;
            case '<': return p < q ? 1 : 0;
            case 'G': return p >= q ? 1 : 0;
            case 'L': return p <= q ? 1 : 0;
            default: return 0;
        }
    }

    static Expr function(final String name, final List<Expr> args) {
        final Expr[] a = args.toArray(new Expr[0]);
        switch (name) {
            case "vec2":
            case "vec3":
            case "vec4": {
                final int n = name.charAt(3) - '0';
                return new Expr() {
                    @Override
                    public double[] eval(final Env env) {
                        final double[] out = new double[n];
                        int k = 0;
                        for (final Expr e : a) {
                            for (final double d : e.eval(env)) {
                                if (k < n) {
                                    out[k++] = d;
                                }
                            }
                        }
                        return out;
                    }
                };
            }
            case "if":
            case "ifb":
                return new Expr() {
                    @Override
                    public double[] eval(final Env env) {
                        int i = 0;
                        for (; i + 1 < a.length; i += 2) {
                            if (a[i].evalScalar(env) != 0) {
                                return a[i + 1].eval(env);
                            }
                        }
                        return i < a.length ? a[i].eval(env) : ZERO;
                    }
                };
            case "smooth": {
                // smooth([id], val, [fadeIn, [fadeOut]]): the optional id is a number literal first
                final boolean id = a.length >= 2 && a[0] instanceof Literal && (a.length >= 3 || !(a[1] instanceof Literal));
                final int o = id ? 1 : 0;
                if (a.length <= o) {
                    return constant(0);
                }
                final Expr val = a[o];
                final Expr in = a.length > o + 1 ? a[o + 1] : constant(1);
                final Expr out = a.length > o + 2 ? a[o + 2] : in;
                return new Expr() {
                    private double value = Double.NaN;
                    private double last;

                    @Override
                    public double[] eval(final Env env) {
                        final double target = val.evalScalar(env);
                        final double now = env.time();
                        if (Double.isNaN(this.value)) {
                            this.value = target;
                            this.last = now;
                            return new double[] {target};
                        }
                        final double dt = Math.max(0, now - this.last);
                        this.last = now;
                        final double fade = target > this.value ? in.evalScalar(env) : out.evalScalar(env);
                        if (fade <= 0) {
                            this.value = target;
                        } else {
                            final double k = 1 - Math.pow(0.01, dt / fade);
                            this.value += (target - this.value) * k;
                        }
                        return new double[] {this.value};
                    }
                };
            }
            case "random":
                return new Expr() {
                    @Override
                    public double[] eval(final Env env) {
                        return new double[] {RANDOM.nextDouble()};
                    }
                };
            case "print":
                return a.length >= 3 ? a[2] : constant(0);
            case "between":
                return scalarFn(a, v -> v[0] >= v[1] && v[0] <= v[2] ? 1 : 0, 3);
            case "equals":
                return scalarFn(a, v -> Math.abs(v[0] - v[1]) <= v[2] ? 1 : 0, 3);
            case "in":
                return scalarFn(a, v -> {
                    for (int i = 1; i < v.length; i++) {
                        if (v[0] == v[i]) {
                            return 1;
                        }
                    }
                    return 0;
                }, -1);
            case "min":
                return scalarFn(a, v -> {
                    double m = v[0];
                    for (final double d : v) {
                        m = Math.min(m, d);
                    }
                    return m;
                }, -1);
            case "max":
                return scalarFn(a, v -> {
                    double m = v[0];
                    for (final double d : v) {
                        m = Math.max(m, d);
                    }
                    return m;
                }, -1);
            case "clamp": return scalarFn(a, v -> Math.max(v[1], Math.min(v[2], v[0])), 3);
            case "sin": return scalarFn(a, v -> Math.sin(v[0]), 1);
            case "cos": return scalarFn(a, v -> Math.cos(v[0]), 1);
            case "asin": return scalarFn(a, v -> Math.asin(v[0]), 1);
            case "acos": return scalarFn(a, v -> Math.acos(v[0]), 1);
            case "tan": return scalarFn(a, v -> Math.tan(v[0]), 1);
            case "atan": return scalarFn(a, v -> Math.atan(v[0]), 1);
            case "atan2": return scalarFn(a, v -> Math.atan2(v[0], v[1]), 2);
            case "torad": return scalarFn(a, v -> Math.toRadians(v[0]), 1);
            case "todeg": return scalarFn(a, v -> Math.toDegrees(v[0]), 1);
            case "abs": return scalarFn(a, v -> Math.abs(v[0]), 1);
            case "floor": return scalarFn(a, v -> Math.floor(v[0]), 1);
            case "ceil": return scalarFn(a, v -> Math.ceil(v[0]), 1);
            case "exp": return scalarFn(a, v -> Math.exp(v[0]), 1);
            case "frac": return scalarFn(a, v -> v[0] - Math.floor(v[0]), 1);
            case "log": return scalarFn(a, v -> Math.log(v[0]), 1);
            case "pow": return scalarFn(a, v -> Math.pow(v[0], v[1]), 2);
            case "round": return scalarFn(a, v -> Math.rint(v[0]), 1);
            case "signum": return scalarFn(a, v -> Math.signum(v[0]), 1);
            case "sqrt": return scalarFn(a, v -> Math.sqrt(v[0]), 1);
            case "fmod": return scalarFn(a, v -> v[1] == 0 ? 0 : v[0] - v[1] * Math.floor(v[0] / v[1]), 2);
            case "lerp": return scalarFn(a, v -> v[1] + (v[2] - v[1]) * v[0], 3);
            default:
                throw new IllegalArgumentException("unknown function " + name + "()");
        }
    }

    private interface Fn {
        double apply(double[] v);
    }

    private static Expr scalarFn(final Expr[] a, final Fn fn, final int arity) {
        if (arity > 0 && a.length < arity) {
            throw new IllegalArgumentException("not enough arguments");
        }
        if (a.length == 0) {
            return constant(0);
        }
        return new Expr() {
            @Override
            public double[] eval(final Env env) {
                final double[] v = new double[a.length];
                for (int i = 0; i < a.length; i++) {
                    v[i] = a[i].evalScalar(env);
                }
                final double r = fn.apply(v);
                return new double[] {Double.isNaN(r) ? 0 : r};
            }
        };
    }
}

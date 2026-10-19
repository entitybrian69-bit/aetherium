"""Helpers to emit era blocks in 1.21.1 form: block(era, [(label, code), ...]) with the first
variant active and the rest commented with //~ (the layout tools/dev/normalize.py expects)."""


def comment(code, base):
    out = []
    for line in code.rstrip("\n").split("\n"):
        if not line.strip():
            out.append(line)
            continue
        ind = len(line) - len(line.lstrip())
        cut = min(ind, base)
        out.append(line[:cut] + "//~ " + line[cut:])
    return "\n".join(out) + "\n"


def block(era, variants, indent=4):
    pad = " " * indent
    out = ""
    for i, (label, code) in enumerate(variants):
        out += "%s// @era:%s-%s %s\n" % (pad, era, "begin" if i == 0 else "else", label)
        if code.strip():
            out += code if i == 0 else comment(code, indent)
    out += "%s// @era:%s-end\n" % (pad, era)
    return out

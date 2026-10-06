#!/bin/sh
# Installs (or points at) the toolchain Aetherium's build needs: JDK 21 and, only if
# requested, Gradle. Deliberately does NOT download a JDK by scraping vendor URLs:
# it uses the platform package manager where one exists, and otherwise prints the
# exact command for your platform. A build script that fetches a compiler from a
# guessed URL is a supply-chain problem, not a convenience.
#
# Usage:
#   sh tools/setup_jdk.sh              # detect and report
#   sh tools/setup_jdk.sh --install     # install via apt/dnf/pacman/brew/winget hint
#   sh tools/setup_jdk.sh --install gradle
set -eu

need_java=21
install=0
want_gradle=0
for arg in "$@"; do
    case "$arg" in
        --install) install=1 ;;
        gradle) want_gradle=1 ;;
        -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
        *) echo "unknown argument: $arg (see --help)" >&2; exit 2 ;;
    esac
done

have() { command -v "$1" >/dev/null 2>&1; }

java_version_of() {
    # $1 = java executable. Prints the major version, or nothing on failure.
    [ -x "$1" ] || return 0
    "$1" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1
}

echo "== Aetherium toolchain setup"
found_java=""
found_version=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    found_java="$JAVA_HOME/bin/java"
    found_version=$(java_version_of "$found_java")
    echo "JAVA_HOME: $JAVA_HOME (java $found_version)"
fi
if [ -z "$found_version" ] && have java; then
    found_java=$(command -v java)
    found_version=$(java_version_of "$found_java")
    echo "PATH java: $found_java (java $found_version)"
fi
if [ -z "$found_version" ] && [ "$install" -eq 1 ]; then
    echo "no JDK found; --install was given, trying the system package manager"
    if have apt-get; then
        # -qq makes apt-get exit 0 while failing, so no quiet flag here: the output
        # is the evidence.
        sudo apt-get update
        sudo apt-get install -y "openjdk-${need_java}-jdk-headless"
    elif have dnf; then
        sudo dnf install -y "java-${need_java}-openjdk-devel"
    elif have pacman; then
        sudo pacman -Sy --noconfirm "jdk${need_java}-openjdk"
    elif have brew; then
        brew install --cask "temurin@${need_java}"
    else
        echo "no supported package manager found (apt-get/dnf/pacman/brew)." >&2
        echo "Install Temurin $need_java manually:" >&2
        echo "  https://adoptium.net/temurin/releases/?version=${need_java}" >&2
        echo "or, on Android (Termux): pkg install openjdk-${need_java}" >&2
        exit 1
    fi
    if have java; then
        found_java=$(command -v java)
        found_version=$(java_version_of "$found_java")
    fi
fi

if [ -z "$found_version" ]; then
    echo "error: no JDK on this machine, and --install was not given." >&2
    echo "Aetherium needs JDK $need_java (the 1.21.1 toolchain pin in gradle.properties)." >&2
    exit 1
fi

if [ "$found_version" -lt "$need_java" ] 2>/dev/null; then
    echo "error: found java $found_version but the build pins $need_java (records/java_version)." >&2
    echo "The Gradle toolchain will try to auto-download $need_java; that needs the" >&2
    echo "'org.gradle.toolchains.foojay-resolver-convention' plugin in settings.gradle.kts," >&2
    echo "or install JDK $need_java directly." >&2
    exit 1
fi
echo "java: ok (major $found_version)"

if [ "$want_gradle" -eq 1 ]; then
    if have gradle; then
        echo "gradle: $(gradle --version 2>/dev/null | sed -n 's/^Gradle //p' | head -n 1)"
    elif [ "$install" -eq 1 ] && have sdk; then
        sdk install gradle "$(sed -n 's/^distributionName=.*-\([0-9][0-9.]*\)-bin.*/\1/p' \
            gradle/wrapper/gradle-wrapper.properties 2>/dev/null | head -n 1 || echo 9.4.1)"
    else
        echo "gradle: not installed. Either" >&2
        echo "  sdk install gradle 9.4.1" >&2
        echo "or download https://services.gradle.org/distributions/gradle-9.4.1-bin.zip" >&2
        echo "Gradle is only needed once, to generate gradle/wrapper/gradle-wrapper.jar." >&2
        exit 1
    fi
    if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
        echo "generating the wrapper jar (this is the step the checkout cannot pre-bake)"
        gradle wrapper --gradle-version 9.4.1
        ls -l gradle/wrapper/gradle-wrapper.jar
    else
        echo "wrapper: gradle/wrapper/gradle-wrapper.jar present"
    fi
fi

echo
echo "== license text"
if [ -f LICENSE ] && ! grep -q "TERMS AND CONDITIONS FOR USE" LICENSE; then
    echo "LICENSE holds the project notice but not the verbatim LGPL text."
    echo "Fetch it before publishing a build:  sh tools/fetch_license.sh"
else
    echo "LICENSE: ok"
fi

echo
echo "done. next: ./gradlew verifyAll   (or: sh tools/checkTree.sh if you only want the offline checks)"

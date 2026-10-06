// The `common` module holds 100% of the engine, GUI and Mixins. It is compiled
// against Mojang mappings with NO loader dependency, which is what makes the
// per-version deltas mechanical (see tools/port.sh).
plugins {
    // Backticks are required: `java-library` is a hyphenated accessor, and bare
    // java-library parses as subtraction (CI said: Unresolved reference 'minus').
    `java-library`
    checkstyle
    // A plugins {} block in a build script is extracted and evaluated before `project`
    // exists, so project.property() there is a compile error. The version is pinned in the
    // catalog, which check.py keeps equal to fabric_loom_version in gradle.properties.
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = "${project.property("archives_base_name")}-common"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(project.property("java_version").toString().toInt())
    }
    withSourcesJar()
}

repositories {
    mavenLocal()
}

// NOTE: `common` intentionally declares no `loom { runs { } }` block. Run
// configurations are generated only by the platform modules (fabric/,
// neoforge/), which are the projects that have a loader dependency.

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    compileOnly("net.fabricmc:sponge-mixin:${project.property("sponge_mixin_version")}")
    annotationProcessor("net.fabricmc:sponge-mixin:${project.property("sponge_mixin_version")}")

    compileOnly("io.github.llamalad7:mixinextras-common:${project.property("mixin_extras_version")}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${project.property("mixin_extras_version")}")

    compileOnly("org.lwjgl:lwjgl:${project.property("lwjgl_version")}")
    compileOnly("org.lwjgl:lwjgl-opengl:${project.property("lwjgl_version")}")
    compileOnly("org.lwjgl:lwjgl-glfw:${project.property("lwjgl_version")}")
    compileOnly("org.lwjgl:lwjgl-vulkan:${project.property("lwjgl_version")}")
    compileOnly("org.lwjgl:lwjgl-stb:${project.property("lwjgl_version")}")
    // Iris/Oculus are deliberately NOT declared here at all: com.aetherium.shader
    // binds the Iris v0 API purely by reflection (docs/IRIS_COMPAT.md), so the
    // jar has zero compile-time or runtime dependency on any shader mod.

    // The unit tests touch engine classes only - config, JSON, gamma, frame stats,
    // backend selection, conflict scanning - so no Minecraft or LWJGL type appears in a
    // test signature and nothing has to be added to the test compile classpath. The
    // runtime classpath does carry them (testRuntimeClasspath extends runtimeClasspath,
    // which is where Loom puts the Minecraft jar), which is what lets a test load a class
    // whose static initialiser logs through the loader.
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        java.srcDirs("src/main/java")
        resources.srcDirs("src/main/resources")
    }
    test {
        java.srcDirs("src/test/java")
        resources.srcDirs("src/test/resources")
    }
}

// Gradle 9 generates task accessors on TaskContainer, not on Project: a bare
// `processResources { }` is an unresolved receiver (CI: "receiver type mismatch").
// `tasks.processResources` keeps the typed ProcessResources receiver, so filesMatching/expand work.
tasks.processResources {
    val props = mapOf(
        "version" to project.property("mod_version"),
        "minecraft_version" to project.property("minecraft_version"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        // NOTE: no `mixin_required` token. aetherium-common.mixins.json is copied
        // verbatim into both platform jars, so a placeholder in it would expand in
        // `common` but stay literal in `fabric`/`neoforge` - two different shipped
        // files from one source. The mixin config is therefore static, and
        // strict_mixins is enforced at runtime by AetheriumMixinPlugin instead.
    )
    filesMatching(listOf("**/*.json", "**/*.toml", "META-INF/*.services")) {
        expand(props)
    }
}

// -D on the Gradle command line lands on the daemon's JVM, not on the forked test JVM,
// so the benchmark switches have to be forwarded explicitly. Without this, `sh
// tools/benchmark.sh` would run zero tests and call that a result.
tasks.withType<Test>().configureEach {
    listOf("aetherium.bench", "aetherium.bench.out").forEach { key ->
        System.getProperty(key)?.let { value -> systemProperty(key, value) }
    }
}

tasks.withType<Jar> {
    // Keep unit tests out of shipped jars.
    exclude("com/aetherium/test/**")
}

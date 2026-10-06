// NeoForge platform shell. Uses ModDevLauncher (net.neoforged.moddev), the same
// plugin NeoForge itself and Embeddium use for 1.21.x.
plugins {
    java
    checkstyle
    alias(libs.plugins.neoforge.moddev)   // version: gradle/libs.versions.toml, see check.py
}

base {
    archivesName = "${project.property("archives_base_name")}-neoforge"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(project.property("java_version").toString().toInt())
    }
}

val commonJava = file("../common/src/main/java")
val commonResources = file("../common/src/main/resources")

sourceSets {
    main {
        java.srcDir(commonJava)
        resources.srcDir(commonResources)
    }
}

    // [UNVERIFIED: the ModDev DSL surface used here (neoForge { version, parchment, runs, mods,
//     unitTest }) for plugin version 2.0.141. Names are read from the pinned NeoForge and
//     Embeddium build scripts of the same era; a DSL rename is a configuration error that the
//     first `./gradlew help` on a real machine will name precisely.]
neoForge {
    version = project.property("neoforge_version").toString()

    parchment {
        // Parchment deobfuscation is opt-in; the reference build works without it
        // so a clean machine never needs the parchment maven.
        if (project.property("parchment_version").toString().isNotBlank()) {
            minecraftVersion = project.property("minecraft_version").toString()
            mappingsVersion = project.property("parchment_version").toString()
        }
    }

    runs {
        if (project.property("aetherium.enableRunConfigs").toString().toBoolean()) {
            create("client") {
                client()
                gameDirectory.set(layout.projectDirectory.dir("run").asFile)
            }
        }
    }

    mods {
        create(project.property("mod_id").toString()) {
            sourceSet(sourceSets["main"])
        }
    }

    // No `unitTest { }` block. ModDev 2.0.141's DSL surface here is not verified from source, and
    // CI proved it matters: `unitTest { enabled = true }` failed *script compilation*, which takes
    // down every task in the build, not just the tests someone would have run. There are no test
    // sources in this module at all (14 test classes live in :common, which owns the test task), so
    // the block bought nothing and cost the whole build. Re-add it only with the property name read
    // out of the pinned plugin's own docs or source.
}

repositories {
    mavenLocal()
}

dependencies {
    compileOnly("io.github.llamalad7:mixinextras-common:${project.property("mixin_extras_version")}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${project.property("mixin_extras_version")}")
    // NeoForge ships Mixin support; MixinExtras must be embedded for it to work.
    implementation("io.github.llamalad7:mixinextras-neoforge:${project.property("mixin_extras_version")}")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

// Gradle 9 generates task accessors on TaskContainer, not on Project: a bare
// `processResources { }` is an unresolved receiver (CI: "receiver type mismatch").
// `tasks.processResources` keeps the typed ProcessResources receiver, so filesMatching/expand work.
tasks.processResources {
    val props = mapOf(
        "version" to project.property("mod_version").toString(),
        "minecraft_version" to project.property("minecraft_version").toString(),
        "neoforge_version" to project.property("neoforge_version").toString(),
        "loader_version_range" to "[4,)",
        "minecraft_version_range" to "[${project.property("minecraft_version")},${nextMinor(project.property("minecraft_version").toString())})",
        "mod_id" to project.property("mod_id").toString(),
        "mod_name" to project.property("mod_name").toString(),
        "mod_description" to project.property("mod_description").toString(),
        "mod_homepage" to project.property("mod_homepage").toString(),
        "mod_sources" to project.property("mod_sources").toString(),
        "mod_issue_tracker" to project.property("mod_issue_tracker").toString(),
        "mod_license" to project.property("mod_license").toString(),
    )
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}

/** "1.21.1" -> "1.22" ; "26.1.2" -> "26.2". Used for the depends range. */
fun nextMinor(v: String): String {
    val parts = v.split(".")
    if (parts.size < 2) {
        return v
    }
    val minor = parts[1].toIntOrNull() ?: return v
    return "${parts[0]}.${minor + 1}"
}

tasks.withType<Jar> {
    // Embed MixinExtras exactly like Embeddium does; NeoForge has no jar-in-jar
    // for the "mod" configuration the way Fabric's `include` does.
    // `from({ ... })` would pass a Kotlin lambda where Gradle expects a Closure/Provider and
    // silently copy nothing. A Provider over the filtered files resolves lazily at execution,
    // which is also why this does not force the configuration to resolve during configuration.
    from(configurations.named("implementation").map { cfg ->
        cfg.copy().files.filter { f -> f.name.contains("mixinextras") }.map { f -> zipTree(f) }
    })
    manifest {
        attributes(
            "Specification-Title" to project.property("mod_id").toString(),
            "Implementation-Title" to project.property("mod_name").toString(),
            "Implementation-Version" to project.property("mod_version").toString(),
        )
    }
}

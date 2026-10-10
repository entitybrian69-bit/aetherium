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

// The ModDev 2.0.141 DSL surface used here (neoForge { version, parchment, runs, mods }) was a
// marked uncertainty until 2026-10-06, when CI's script compilation resolved every call: the names
// and signatures exist as written. What stays unverified is what they *do* (parchment output,
// run-config wiring) - that needs a real client launch. One call was removed on the same evidence:
// `unitTest { enabled = ... }` is not this plugin's surface, and it failed script compilation,
// which takes down every task in the build, not just the tests nobody would have run here.
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

// A resolvable view over `implementation`. NeoForge has no jar-in-jar for the mod's own
// dependencies, so MixinExtras is unpacked into this jar (as Embeddium does) - and reading a
// declarable-only configuration directly is an illegal call, so this is the configuration that is
// allowed to resolve. Created before `dependencies` below so the extendsFrom edge exists first.
val embeddable = configurations.create("embeddableRuntime") {
    extendsFrom(configurations.getByName("implementation"))
    isCanBeConsumed = false
    isCanBeResolved = true
    description = "Files unpacked into the neoforge jar (MixinExtras only); see tasks.withType<Jar>."
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
        // Exact: each jar carries the era blocks of the one version it was compiled against.
        "minecraft_version_range" to "[${project.property("minecraft_version")}]",
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

tasks.withType<Jar> {
    // Embed MixinExtras exactly like Embeddium does; NeoForge has no jar-in-jar
    // for the "mod" configuration the way Fabric's `include` does.
    // `elements` (not `.files`, not `.copy()`) so nothing resolves during configuration. CI named
    // the earlier form: "Calling configuration method 'copy()' is not allowed for configuration
    // 'implementation'" - it is declarable-only, hence the resolvable view declared above.
    from(project.provider {
        // `embeddable.elements.map { }` failed inference (Kotlin could not pin the element type,
        // CI: "Unresolved reference 'name'"). project.provider keeps the same laziness - the
        // configuration resolves at execution, not during configuration - with a lambda whose
        // receiver types are plain JDK ones.
        embeddable.files.filter { f -> f.name.contains("mixinextras") }.map { f -> zipTree(f) }
    })
    manifest {
        attributes(
            "Specification-Title" to project.property("mod_id").toString(),
            "Implementation-Title" to project.property("mod_name").toString(),
            "Implementation-Version" to project.property("mod_version").toString(),
        )
    }
}

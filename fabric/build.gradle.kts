// Fabric platform shell. The common sources are compiled *into* this module via
// srcDir so that no intermediate jar needs remapping twice — this is the
// MultiLoader pattern and it keeps the per-version deltas tiny.
plugins {
    java
    checkstyle
    alias(libs.plugins.fabric.loom)   // version: gradle/libs.versions.toml, see check.py
}

base {
    archivesName = "${project.property("archives_base_name")}-fabric"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(project.property("java_version").toString().toInt())
    }
    withSourcesJar()
}

val commonJava = file("../common/src/main/java")
val commonResources = file("../common/src/main/resources")

sourceSets {
    main {
        java.srcDir(commonJava)
        resources.srcDir(commonResources)
    }
}

loom {
    mods {
        create("aetherium") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["test"])
        }
    }
    // Run configurations are opt-in (aetherium.enableRunConfigs=false by default) because the exact
    // Loom 1.16 run DSL for this module's shape is not verified from upstream source here. Note what
    // CI taught us on 2026-10-06: naming a run-config property that Loom does not declare
    // (`configName`, `sourceRuns`) is not a runtime-only risk - Kotlin resolves the call at *script
    // compile* time, inside the `if`, so an unverified name broke the build for everybody. Keep this
    // block to calls verified against Loom's own docs/example mod; add the rest only with a source.
    if (project.property("aetherium.enableRunConfigs").toString().toBoolean()) {
        runs {
            create("client") {
                client()
            }
        }
    }
}

repositories {
    mavenLocal()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    // Fabric API is compile-only and optional: every hook Aetherium needs is a
    // Mixin (see aetherium-common.mixins.json), which is what lets the same jar run
    // on a client with no Fabric API installed. No source file imports it.
    modCompileOnly("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")

    // Mixin plugin + extras are provided by the loader at runtime; we only need
    // them to compile against, then we embed the API surface we actually use.
    compileOnly("io.github.llamalad7:mixinextras-common:${project.property("mixin_extras_version")}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${project.property("mixin_extras_version")}")
    include("io.github.llamalad7:mixinextras-fabric:${project.property("mixin_extras_version")}")

    // NOTE: deliberately NO Mod Menu compile dependency. Aetherium's screen is
    // reachable from vanilla Options -> Video Settings (OptionsScreenMixin, an
    // injection point verified against Sodium 1.21.1) and from the Aetherium
    // keybind. An optional third-party menu entry would only add a pin we
    // cannot verify offline. To add it: declare `maven.modrinth` in
    // settings.gradle.kts and implement com.terraformersmc.modmenu.api.ModMenuApi.

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

// Gradle 9 generates task accessors on TaskContainer, not on Project: a bare
// `processResources { }` is an unresolved receiver (CI: "receiver type mismatch").
// `tasks.processResources` keeps the typed ProcessResources receiver, so filesMatching/expand work.
tasks.processResources {
    val props = mapOf(
        "version" to project.property("mod_version"),
        "minecraft_version" to project.property("minecraft_version"),
        "loader_version" to project.property("fabric_loader_version"),
        "fabric_api_version" to project.property("fabric_api_version"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_description" to project.property("mod_description"),
        "mod_homepage" to project.property("mod_homepage"),
        "mod_sources" to project.property("mod_sources"),
        "mod_issue_tracker" to project.property("mod_issue_tracker"),
        "mod_license" to project.property("mod_license"),
    )
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.withType<net.fabricmc.loom.task.RemapJarTask> {
    addNestedDependencies = true
}

tasks.named("build") {
    dependsOn("remapJar")
}

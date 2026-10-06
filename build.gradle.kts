plugins {
    id("java")
    id("idea")
}

val modId: String by project
val modVersion: String by project
val modGroup: String by project

subprojects {
    apply(plugin = "java")
    apply(plugin = "idea")

    val javaVersion = (project.property("java_version") as String).toInt()

    group = project.property("mod_group") as String
    version = "${project.property("mod_version")}+${project.property("minecraft_version")}"

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
        withSourcesJar()
    }

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://maven.parchmentmc.org")
        maven("https://api.modrinth.com/maven") {
            content { includeGroup("maven.modrinth") }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(javaVersion)
    }

    tasks.withType<ProcessResources>().configureEach {
        val props = mapOf(
            "mod_id" to project.property("mod_id"),
            "mod_name" to project.property("mod_name"),
            "mod_version" to project.property("mod_version"),
            "mod_author" to project.property("mod_author"),
            "mod_description" to project.property("mod_description"),
            "mod_license" to project.property("mod_license"),
            "minecraft_version" to project.property("minecraft_version"),
            "minecraft_version_range" to project.property("minecraft_version_range"),
            "fabric_loader_version_range" to project.property("fabric_loader_version_range"),
            "neoforge_version_range" to project.property("neoforge_version_range"),
            "neoforge_loader_version_range" to project.property("neoforge_loader_version_range"),
            "java_version" to project.property("java_version")
        )
        inputs.properties(props)
        filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
            expand(props)
        }
    }

    tasks.withType<Jar>().configureEach {
        from(rootProject.file("LICENSE")) { rename { "${it}_${project.property("mod_name")}" } }
        manifest {
            attributes(
                "Specification-Title" to project.property("mod_name"),
                "Specification-Vendor" to project.property("mod_author"),
                "Specification-Version" to project.property("mod_version"),
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.property("mod_version"),
                "Implementation-Vendor" to project.property("mod_author")
            )
        }
    }
}

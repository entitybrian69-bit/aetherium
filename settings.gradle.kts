pluginManagement {
    repositories {
        mavenLocal()
        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases/")
        }
        maven {
            name = "SpongePowered"
            url = uri("https://repo.spongepowered.org/maven/")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases/")
        }
        maven {
            name = "SpongePowered"
            url = uri("https://repo.spongepowered.org/maven/")
        }
        maven {
            name = "Su5ed (moddev patches)"
            url = uri("https://maven.su5ed.dev/releases")
        }
        mavenCentral()
    }
}

rootProject.name = "aetherium"

include("common")
include("fabric")

// NeoForge begins at Minecraft 1.20.2. Rows below that set enabled_platforms=fabric in
// gradle.properties (the porting deltas do this), and Gradle configures every included
// project before running any task - so a Fabric-only tree must not include the module
// at all, or :neoforge would fail to resolve its (nonexistent) NeoForge build during
// configuration and take :common and :fabric down with it.
if (providers.gradleProperty("enabled_platforms").orElse("fabric,neoforge").get().split(",").contains("neoforge")) {
    include("neoforge")
}

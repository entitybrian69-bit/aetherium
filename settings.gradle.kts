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
include("neoforge")

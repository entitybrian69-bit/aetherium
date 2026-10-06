plugins {
    id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT"
}

val minecraftVersion: String = project.property("minecraft_version") as String
val lwjglVersion: String = project.property("lwjgl_version") as String

minecraft {
    version(minecraftVersion)
    accessWideners(file("src/main/resources/aetherium.accesswidener"))
}

dependencies {
    compileOnly("org.spongepowered:mixin:${project.property("mixin_version")}")
    compileOnly("io.github.llamalad7:mixinextras-common:${project.property("mixinextras_version")}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${project.property("mixinextras_version")}")
    // Vulkan bindings are not shipped by vanilla; loaders bundle them via jar-in-jar.
    compileOnly("org.lwjgl:lwjgl-vulkan:$lwjglVersion")
    compileOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    compileOnly("com.google.code.gson:gson:${project.property("gson_version")}")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("common")
}

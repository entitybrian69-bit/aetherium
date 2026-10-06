plugins {
    id("fabric-loom") version "1.8-SNAPSHOT"
}

val minecraftVersion: String = project.property("minecraft_version") as String
val lwjglVersion: String = project.property("lwjgl_version") as String

loom {
    accessWidenerPath.set(project(":common").file("src/main/resources/aetherium.accesswidener"))
    mixin { defaultRefmapName.set("aetherium.refmap.json") }
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
            vmArg("-Daetherium.debug=true")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")

    implementation(project(":common", configuration = "namedElements")) { isTransitive = false }
    "include"(implementation("org.lwjgl:lwjgl-vulkan:$lwjglVersion")!!)
}

tasks.named<ProcessResources>("processResources") {
    from(project(":common").sourceSets.main.get().resources)
}

tasks.named<JavaCompile>("compileJava") {
    source(project(":common").sourceSets.main.get().allSource)
}

tasks.named<Jar>("sourcesJar") {
    from(project(":common").sourceSets.main.get().allSource)
}

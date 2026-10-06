plugins {
    id("net.neoforged.moddev") version "1.0.21"
}

val minecraftVersion: String = project.property("minecraft_version") as String
val lwjglVersion: String = project.property("lwjgl_version") as String
val modId: String = project.property("mod_id") as String

neoForge {
    version = project.property("neoforge_version") as String
    accessTransformers.from(project(":common").file("src/main/resources/META-INF/accesstransformer.cfg"))
    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            jvmArgument("-Daetherium.debug=true")
        }
    }
    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":common").sourceSets.main.get())
        }
    }
}

val jarJar: Configuration by configurations.getting

dependencies {
    implementation(project(":common"))
    jarJar("org.lwjgl:lwjgl-vulkan:[$lwjglVersion,)") { version { prefer(lwjglVersion) } }
    implementation("org.lwjgl:lwjgl-vulkan:$lwjglVersion")
}

tasks.named<ProcessResources>("processResources") {
    from(project(":common").sourceSets.main.get().resources)
}

tasks.named<JavaCompile>("compileJava") {
    source(project(":common").sourceSets.main.get().allSource)
}

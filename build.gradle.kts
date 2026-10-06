import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

// Root project: style enforcement + aggregate tasks only. All Minecraft-facing
// configuration lives in common/fabric/neoforge so that the per-version delta
// patches (deltas/<version>/build.gradle.kts) stay small and textual.
plugins {
    java
    checkstyle
}

allprojects {
    group = property("maven_group").toString()
    version = property("mod_version").toString()

    defaultTasks("build")
}

subprojects {
    apply(plugin = "checkstyle")

    extensions.configure<CheckstyleExtension> {
        toolVersion = "10.20.1"
        configFile = rootProject.file("checkstyle.xml")
        maxWarnings = 0
        // Every module uses the same ruleset; violations fail the build in CI.
        isIgnoreFailures = false
    }

    tasks.withType<Checkstyle>().configureEach {
        reports {
            xml.required = true
            html.required = false
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // -Xlint:all surfaces unused casts / raw types; the renderer is
        // unsafe-heavy so we deliberately do NOT fail on those.
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing", "-parameters"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }
}

/** Aggregate: build every platform jar for the reference version. */
tasks.register("buildAll") {
    group = "aetherium"
    description = "Builds common, fabric and neoforge artifacts for the reference version."
    dependsOn(":common:build", ":fabric:build", ":neoforge:build")
}

/** Aggregate: the CI entry point (see .github/workflows/build.yml). */
tasks.register("verifyAll") {
    group = "aetherium"
    description = "Runs every verification: compile, unit tests, static tree checks, resource validation."
    dependsOn("buildAll", ":common:test", "checkTree")
}

tasks.register("checkTree") {
    group = "aetherium"
    description = "Runs the offline structural verifier (tools/check.py) — no JDK or maven needed."
    val script = rootProject.file("tools/check.py")
    val output = layout.buildDirectory.file("reports/aetherium/tree-check.txt")
    outputs.file(output)
    inputs.file(script)
    doLast {
        val target = output.get().asFile
        target.parentFile.mkdirs()
        val proc = ProcessBuilder("python3", script.absolutePath, rootProject.projectDir.absolutePath)
            .redirectErrorStream(true)
            .start()
        val text = proc.inputStream.bufferedReader().readText()
        target.writeText(text)
        print(text)
        val code = proc.waitFor()
        if (code != 0) {
            throw GradleException("tools/check.py reported structural errors (exit $code); see ${target.absolutePath}")
        }
    }
}

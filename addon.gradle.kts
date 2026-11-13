// Test and coverage infrastructure for The Aether Legacy.
//
// Kept in an addon script so the generated GTNH build files stay untouched by
// `updateBuildscript`. The unit-test suite is plain JUnit 4 + Mockito on a Java 8
// toolchain, and JaCoCo produces an HTML/XML baseline on every `check`.

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

apply(plugin = "jacoco")

project.extensions.configure(JacocoPluginExtension::class.java) {
    toolVersion = "0.8.16"
}

val mainSourceSet = project.extensions.getByType(SourceSetContainer::class.java)
    .getByName("main")

// Production classes implement compileOnly supertypes in some environments.
// Tests need the same compile/runtime classpath as main so they can name those
// classes and boot the same Minecraft classes.
configurations.named("testCompileClasspath") {
    extendsFrom(configurations.named("compileOnly").get())
}
configurations.named("testRuntimeClasspath") {
    extendsFrom(configurations.named("compileOnly").get())
}

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isEnabled = true
    }
    javaLauncher.set(
        project.extensions.getByType(JavaToolchainService::class.java)
            .launcherFor {
                languageVersion.set(JavaLanguageVersion.of(8))
            }
    )
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates HTML and XML JaCoCo coverage baselines from the unit tests"
    dependsOn(tasks.named("test"))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(mainSourceSet.output.classesDirs)
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(
        project.files(project.layout.buildDirectory.file("jacoco/test.exec"))
    )
}

// Generate the baseline report whenever a normal build/check runs.
tasks.named("check") {
    dependsOn(tasks.named("jacocoTestReport"))
}

// ---------------------------------------------------------------------------------------------------------
// In-game test mod (never shipped)
// ---------------------------------------------------------------------------------------------------------

val gameTestSourceSet = project.extensions.getByType(SourceSetContainer::class.java)
    .create("gameTest")

configurations.getByName(gameTestSourceSet.compileClasspathConfigurationName)
    .extendsFrom(configurations.getByName("compileClasspath"))
configurations.getByName(gameTestSourceSet.runtimeClasspathConfigurationName)
    .extendsFrom(configurations.getByName("runtimeClasspath"))

gameTestSourceSet.compileClasspath += mainSourceSet.output
gameTestSourceSet.compileClasspath += mainSourceSet.compileClasspath
gameTestSourceSet.runtimeClasspath += mainSourceSet.output
gameTestSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath

// The game-test mod is plain Java 8. Leaving Jabel's compiler plugin on this
// source set's classpath makes javac auto-load it through ServiceLoader.
gameTestSourceSet.compileClasspath = gameTestSourceSet.compileClasspath
    .filter { !it.name.contains("jabel") }

val isGameTestRun = project.hasProperty("gameTest") ||
    gradle.startParameter.taskNames.any { it.contains("gameTestServer") }

if (isGameTestRun) {
    tasks.withType<JavaExec>().configureEach {
        if (name.startsWith("runServer") || name.startsWith("runClient")) {
            classpath += gameTestSourceSet.output
            systemProperty("aether.gametest", "true")
            // A reused dev world may reference removed blocks/mods; answer FML's
            // "continue anyway?" prompt up front instead of blocking on stdin.
            systemProperty("fml.queryResult", "confirm")
        }
    }
}

tasks.named("check") {
    dependsOn(gameTestSourceSet.classesTaskName)
}

val prepareGameTestServer = tasks.register("prepareGameTestServer") {
    val configuredRunDir = project.findProperty("runServerWorkingDirectory") as String? ?: "run/server"
    val serverDir = project.layout.projectDirectory.dir(configuredRunDir).asFile

    group = "verification"
    description = "Creates the dedicated-server run directory and accepts the Minecraft EULA for game tests"

    doLast {
        serverDir.mkdirs()
        val eula = serverDir.resolve("eula.txt")
        if (!eula.exists()) {
            eula.writeText("eula=true\n")
        }
    }
}

tasks.register("gameTestServer") {
    group = "verification"
    description = "Boots a dedicated server, runs the Aether in-game test mod, and exits with its result"
    dependsOn(prepareGameTestServer)
    dependsOn(tasks.named("runServer"))
}

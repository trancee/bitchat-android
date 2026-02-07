// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.whenTaskAdded {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}

val javaToolchains = extensions.getByType<JavaToolchainService>()

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        javaCompiler.set(
            javaToolchains.compilerFor {
                languageVersion.set(JavaLanguageVersion.of(8))
            }
        )
    }

    plugins.withId("org.jetbrains.kotlin.android") {
        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain(8)
        }
    }
}

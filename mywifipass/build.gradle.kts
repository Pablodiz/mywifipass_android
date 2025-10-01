import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
    kotlin("plugin.serialization") version "1.9.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        allWarningsAsErrors = false  // muestra warnings, no falla
        freeCompilerArgs += listOf("-Xjsr305=strict")
    }
}

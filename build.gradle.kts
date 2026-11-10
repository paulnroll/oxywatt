buildscript {
    if (providers.gradleProperty("karooDiagnostic").orNull == "true") {
        repositories {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
        dependencies { classpath("com.android.tools.build:gradle:9.0.1") }
    }
}

plugins {
    base
    kotlin("jvm") version "2.3.20" apply false
    id("com.diffplug.spotless") version "8.11.0"
}

repositories { mavenCentral() }

spotless {
    kotlin {
        target("core/src/**/*.kt", "app/src/**/*.kt")
        ktfmt("0.64").kotlinlangStyle()
    }
    kotlinGradle {
        target("*.gradle.kts", "core/*.gradle.kts", "app/*.gradle.kts")
        ktfmt("0.64").kotlinlangStyle()
    }
}

tasks.named("check") { dependsOn(":core:check") }

project(":core") {
    tasks
        .matching { it.name == "check" }
        .configureEach { dependsOn(rootProject.tasks.named("spotlessCheck")) }
}

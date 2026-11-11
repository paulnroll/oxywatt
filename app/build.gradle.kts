plugins { id("com.android.application") }

repositories {
    google()
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://jitpack.io") }
        filter {
            includeGroup("com.github.nikosavola.karoo-ext-testing")
            if (providers.gradleProperty("karooSdkJitpack").orNull == "true") {
                includeModule("com.github.hammerheadnav", "karoo-ext")
            }
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "karooPackages"
                url = uri("https://maven.pkg.github.com/hammerheadnav/karoo-ext")
                credentials {
                    username = providers.environmentVariable("KAROO_PACKAGES_USERNAME").orNull
                    password = providers.environmentVariable("KAROO_PACKAGES_TOKEN").orNull
                }
            }
        }
        filter { includeModule("io.hammerhead", "karoo-ext") }
    }
}

android {
    namespace = "com.oxywatt.diagnostic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oxywatt.diagnostic"
        minSdk = 23
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(project(":core"))
    val sdk =
        if (providers.gradleProperty("karooSdkJitpack").orNull == "true")
            "com.github.hammerheadnav:karoo-ext:1.1.9"
        else "io.hammerhead:karoo-ext:1.1.9"
    implementation(sdk)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("com.github.nikosavola.karoo-ext-testing:karoo-ext-testing:0.1.1")
    testImplementation(
        "com.github.nikosavola.karoo-ext-testing:karoo-ext-testing-robolectric:0.1.1"
    )
}

// A release must use exactly one SDK and cannot contain test hosts or their transitive tools.
tasks.register("verifyReleaseDependencyIsolation") {
    doLast {
        val artifacts =
            configurations
                .getByName("releaseRuntimeClasspath")
                .resolvedConfiguration
                .resolvedArtifacts
        check(artifacts.count { it.name == "karoo-ext" } == 1) { "Expected exactly one Karoo SDK" }
        check(
            artifacts.none {
                it.moduleVersion.id.group.contains("karoo-ext-testing") ||
                    it.moduleVersion.id.group.startsWith("org.robolectric") ||
                    it.moduleVersion.id.group == "junit"
            }
        ) {
            "Test tooling leaked into release"
        }
        logger.lifecycle(
            "Release dependency isolation verified: one SDK, no fake/Robolectric/JUnit artifacts"
        )
    }
}

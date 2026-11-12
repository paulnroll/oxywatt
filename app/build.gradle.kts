import java.util.zip.ZipFile

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
        versionCode = 5
        versionName = "0.5.0"
        val codeRevision =
            providers
                .exec { commandLine("git", "rev-parse", "HEAD") }
                .standardOutput
                .asText
                .get()
                .trim()
        buildConfigField("String", "CODE_REVISION", "\"$codeRevision\"")
        val codeDirty =
            providers
                .exec { commandLine("git", "status", "--porcelain") }
                .standardOutput
                .asText
                .get()
                .isNotBlank()
        buildConfigField("boolean", "CODE_DIRTY", codeDirty.toString())
        testInstrumentationRunner = "com.oxywatt.diagnostic.preview.PreviewReplayInstrumentation"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testBuildType = "preview"

    buildTypes {
        create("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".preview"
            matchingFallbacks += "debug"
        }
    }
    sourceSets
        .getByName("preview")
        .assets
        .srcDir(layout.buildDirectory.dir("syntheticPreviewAssets").get().asFile)
    buildFeatures { buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(project(":core"))
    "previewImplementation"(testFixtures(project(":core")))
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

androidComponents {
    beforeVariants(selector().all()) {
        if (it.buildType in setOf("debug", "preview")) {
            (it as com.android.build.api.variant.HasUnitTestBuilder).enableUnitTest = true
        }
    }
}

tasks.register("verifyPreviewIsolation") {
    dependsOn("assemblePreview", "assembleRelease")
    doLast {
        val manifest =
            file(
                    "build/intermediates/merged_manifests/preview/processPreviewManifest/AndroidManifest.xml"
                )
                .readText()
        for (component in listOf("MainActivity", "DiagnosticExtension")) {
            check(
                Regex(
                        "<(?:activity|service)[^>]*android:name=\"[^\"]*$component\"[^>]*>",
                        RegexOption.DOT_MATCHES_ALL,
                    )
                    .find(manifest)
                    ?.value
                    ?.contains("android:enabled=\"false\"") == true
            )
        }
        val apk = file("build/outputs/apk/release/app-release-unsigned.apk")
        ZipFile(apk).use { zip ->
            check(zip.entries().asSequence().none { it.name.startsWith("assets/fit-") })
            zip.entries()
                .asSequence()
                .filter { it.name.endsWith(".dex") }
                .forEach {
                    val bytes =
                        zip.getInputStream(it).use { input ->
                            input.readBytes().toString(Charsets.ISO_8859_1)
                        }
                    check(
                        listOf(
                                "PreviewActivity",
                                "SimulatedRideDataSource",
                                "VirtualTimeline",
                                "FakeKaroo",
                            )
                            .none(bytes::contains)
                    )
                }
        }
    }
}

val prepareSyntheticPreviewAssets by
    tasks.registering(Sync::class) {
        from("../core/src/test/resources") {
            include("fit-scenario/manifest.json", "fit-scenario/observations.tsv")
        }
        into(layout.buildDirectory.dir("syntheticPreviewAssets"))
        doFirst {
            check(
                file("../core/src/test/resources/fit-scenario/manifest.json")
                    .readText()
                    .contains("generated-synthetic-v1")
            )
        }
    }

tasks
    .matching { it.name == "prePreviewBuild" }
    .configureEach { dependsOn(prepareSyntheticPreviewAssets) }

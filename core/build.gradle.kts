plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

repositories { mavenCentral() }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        // Bytecode targeting alone does not prevent references to newer JDK APIs.
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach { options.release.set(17) }

dependencies { testImplementation(kotlin("test-junit")) }

// Changing opt-in private fixtures must invalidate the local test result, even if code is
// unchanged.
tasks.test {
    val privateReplay = providers.environmentVariable("OXYWATT_PRIVATE_FIT_REPLAY_DIR")
    inputs.property("privateFitReplayDirectory", privateReplay.orElse(""))
    privateReplay.orNull?.let {
        inputs.files(fileTree(it)).withPropertyName("privateFitReplayFixtures")
    }
}

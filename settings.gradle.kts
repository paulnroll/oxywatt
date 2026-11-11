pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "oxywatt"

include(":core")

if (providers.gradleProperty("karooDiagnostic").orNull == "true") {
    include(":app")
}

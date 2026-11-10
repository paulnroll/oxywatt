plugins { id("com.android.application") }

repositories {
    google()
    mavenCentral()
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { buildConfig = true }
}

dependencies { implementation("io.hammerhead:karoo-ext:1.1.9") }

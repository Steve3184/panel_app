import java.net.URL

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "top.steve3184.panel"
    compileSdk = 36

    defaultConfig {
        applicationId = "top.steve3184.panel"
        minSdk = 28
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.webkit)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)

    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-websockets-jvm:2.3.12")
}

tasks.register("updateFrontend") {
    group = "frontend"
    description = "Downloads and extracts the latest frontend package from GitHub Release."

    doLast {
        val downloadUrl = "https://github.com/Steve3184/panel/releases/download/latest/release-frontend.zip"
        val destinationZipFile = file("${layout.buildDirectory.get()}/frontend.zip")
        val extractDir = file("src/main/assets/")

        println("Downloading frontend from: $downloadUrl")

        try {
            URL(downloadUrl).openStream().use { input ->
                destinationZipFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            println("Frontend package downloaded to: ${destinationZipFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to download frontend package: ${e.message}")
            throw GradleException("Frontend download failed.", e)
        }

        if (extractDir.exists()) {
            println("Cleaning old frontend files in: ${extractDir.absolutePath}")
            extractDir.deleteRecursively()
        }
        extractDir.mkdirs()

        try {
            copy {
                from(zipTree(destinationZipFile))
                into(extractDir)
            }
            println("Frontend package extracted to: ${extractDir.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to extract frontend package: ${e.message}")
            throw GradleException("Frontend extraction failed.", e)
        }

        if (destinationZipFile.exists()) {
            destinationZipFile.delete()
            println("Cleaned up temporary zip file: ${destinationZipFile.absolutePath}")
        }

        println("Frontend update complete.")
    }
}
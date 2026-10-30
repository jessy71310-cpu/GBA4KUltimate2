plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "fr.gba4kultimate"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.gba4kultimate"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-prototype"
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
    kotlinOptions { jvmTarget = "17" }
}
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Planned emulator frontend dependency. The mGBA libretro core (.so) still
    // needs to be built/bundled and wired to the frontend before gameplay works.
    implementation("com.github.swordfish90:libretrodroid:0.13.2")
}

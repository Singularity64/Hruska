plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "cz.hruska.app"
    compileSdk = 35
    defaultConfig { applicationId = "cz.hruska.app"; minSdk = 26; targetSdk = 35; versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1); versionName = "0.1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "1") }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
}
plugins {
    id("movers.android.application")
    id("movers.android.compose")
    id("movers.android.unit-testing")
}
android {
    namespace = "com.jcotters.movers"
    defaultConfig {
        applicationId = "com.jcotters.movers"
        versionCode = 2
        versionName = "2.0"
    }
    buildTypes.getByName("release") {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

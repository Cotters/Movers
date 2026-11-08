plugins {
    id("movers.android.library")
    id("movers.android.compose")
    id("movers.android.hilt")
    id("movers.android.unit-testing")
}

android {
    namespace = "com.jcotters.features.watchlists"
}

dependencies {
    implementation(project(":navigation:contract"))
    implementation(project(":features:watchlists:contract"))
    implementation(project(":auth:contract"))
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}

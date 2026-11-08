plugins {
    id("movers.android.test")
    id("movers.android.compose")
}

android {
    namespace = "com.jcotters.tests"

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.runner)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.test.junit4)
    implementation(project(path = ":auth"))

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

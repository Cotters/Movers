plugins {
    id("movers.android.library")
    id("movers.android.compose")
    id("movers.android.hilt")
    id("movers.android.unit-testing")
}

android {
    namespace = "com.jcotters.features.catalogue"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":features:details:contract"))

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.viewmodel.compose)
    implementation(project(":navigation:contract"))
    implementation(project(":features:catalogue:contract"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.paging.compose)

    implementation(project(path = ":presentation"))
    implementation(project(path = ":movie:contract"))

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

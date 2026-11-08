plugins {
    id("movers.android.library")
    id("movers.android.unit-testing")
}

android {
    namespace = "com.jcotters.movie.contract"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.hilt)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

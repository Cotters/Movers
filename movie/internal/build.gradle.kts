plugins {
    id("movers.android.library")
    id("movers.android.hilt")
    id("movers.android.unit-testing")
}

android {
    namespace = "com.jcotters.movie.internal"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.retrofit.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.room)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(project(path = ":movie:contract"))
    implementation(project(path = ":database"))

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

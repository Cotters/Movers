plugins {
    id("movers.android.library")
    id("movers.kotlin.serialization")
}

android {
    namespace = "com.jcotters.watchlists.contract"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlin.serialization.json)
}

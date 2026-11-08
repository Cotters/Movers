plugins {
    id("movers.android.library")
}

android {
    namespace = "com.jcotters.navigation.contract"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
}

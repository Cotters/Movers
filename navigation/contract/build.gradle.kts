plugins {
    id("movers.multiplatform.library")
    id("movers.kotlin.serialization")
}

kotlin {
    android { namespace = "com.jcotters.navigation.contract" }
    sourceSets.commonMain.dependencies {
        api(libs.androidx.navigation3.runtime)
        api(libs.multiplatform.navigation3.ui)
        implementation(libs.kotlin.serialization.json)
    }
}

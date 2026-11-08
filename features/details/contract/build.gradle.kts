plugins {
    id("movers.multiplatform.library")
    id("movers.kotlin.serialization")
}

kotlin {
    android { namespace = "com.jcotters.details.contract" }
    sourceSets.commonMain.dependencies {
        api(libs.androidx.navigation3.runtime)
        implementation(libs.kotlin.serialization.json)
    }
}

plugins {
    id("movers.multiplatform.library")
    id("movers.kotlin.serialization")
}

kotlin {
    android { namespace = "com.jcotters.movie.contract" }
    sourceSets.commonMain.dependencies {
        api(libs.androidx.paging.common)
        api(libs.kotlinx.coroutines.core)
        implementation(libs.kotlin.serialization.json)
    }
}

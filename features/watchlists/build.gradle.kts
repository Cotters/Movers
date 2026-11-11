plugins {
    id("movers.multiplatform.compose")
}

kotlin {
    android { namespace = "com.jcotters.features.watchlists" }
    sourceSets.androidMain.dependencies { implementation(libs.androidx.activity.compose) }
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.multiplatform.lifecycle.compose)
        implementation(libs.koin.compose.viewmodel)
        implementation(project(":movie:contract"))
        implementation(project(":presentation"))
        implementation(project(":features:details:contract"))
        implementation(project(":features:search:contract"))
        implementation(libs.multiplatform.navigation3.ui)
        implementation(project(":navigation:contract"))
        implementation(project(":features:watchlists:contract"))
    }
}

compose.resources {
    packageOfResClass = "com.jcotters.features.watchlists.resources"
    publicResClass = true
}

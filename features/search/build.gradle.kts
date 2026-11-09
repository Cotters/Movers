plugins { id("movers.multiplatform.compose") }
kotlin {
    android { namespace = "com.jcotters.features.search" }
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.multiplatform.navigation3.ui)
        implementation(libs.multiplatform.lifecycle.compose)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.androidx.paging.compose)
        implementation(project(":presentation"))
        implementation(project(":movie:contract"))
        implementation(project(":navigation:contract"))
        implementation(project(":features:search:contract"))
        implementation(project(":features:details:contract"))
        implementation(project(":features:watchlists:contract"))
    }
}

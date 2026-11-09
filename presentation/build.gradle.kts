plugins {
    id("movers.multiplatform.compose")
}

kotlin {
    android { namespace = "com.jcotters.presentation" }
    sourceSets.commonMain.dependencies {
        api(project(":movie:contract"))
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.multiplatform.navigation3.ui)
        implementation(libs.multiplatform.lifecycle.compose)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
    }
}

compose.resources {
    packageOfResClass = "com.jcotters.presentation.resources"
    publicResClass = true
}

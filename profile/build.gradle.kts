plugins {
    id("movers.multiplatform.compose")
}

kotlin {
    android { namespace = "com.jcotters.profile" }
    sourceSets.commonMain.dependencies {
        implementation(project(":designsystem"))
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.multiplatform.navigation3.ui)
        implementation(libs.multiplatform.lifecycle.compose)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.koin.core)
        implementation(project(":database"))
        implementation(project(":presentation"))
        implementation(project(":navigation:contract"))
        implementation(project(":profile:contract"))
    }
}

compose.resources {
    packageOfResClass = "com.jcotters.profile.resources"
    publicResClass = true
}

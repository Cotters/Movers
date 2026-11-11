plugins {
    id("movers.multiplatform.compose")
}

kotlin {
    android { namespace = "com.jcotters.features.details" }
    sourceSets.named("androidHostTest") { kotlin.srcDir("src/test/java") }
    sourceSets.commonMain.dependencies {
        implementation(project(":designsystem"))
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.multiplatform.navigation3.ui)
        implementation(libs.multiplatform.lifecycle.compose)
        implementation(libs.koin.compose.viewmodel)
        implementation(project(":presentation"))
        implementation(project(":navigation:contract"))
        implementation(project(":movie:contract"))
        implementation(project(":features:details:contract"))
        implementation(project(":features:watchlists:contract"))
    }
}

compose.resources {
    packageOfResClass = "com.jcotters.features.details.resources"
    publicResClass = true
}

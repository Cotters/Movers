plugins {
    id("movers.multiplatform.compose")
}

kotlin {
    android { namespace = "com.jcotters.features.catalogue" }
    sourceSets.named("androidHostTest") { kotlin.srcDir("src/test/java") }
    sourceSets.commonMain.dependencies {
        implementation(project(":designsystem"))
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.multiplatform.navigation3.ui)
        implementation(libs.multiplatform.lifecycle.compose)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.androidx.paging.compose)
        implementation(project(":presentation"))
        implementation(project(":navigation:contract"))
        implementation(project(":movie:contract"))
        implementation(project(":features:catalogue:contract"))
        implementation(project(":features:details:contract"))
        implementation(project(":features:watchlists:contract"))
        implementation(project(":features:search:contract"))
    }
}

compose.resources {
    packageOfResClass = "com.jcotters.features.catalogue.resources"
    publicResClass = true
}

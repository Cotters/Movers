plugins {
    id("movers.multiplatform.compose")
}

kotlin {
    android { namespace = "com.jcotters.features.watchlists" }
    sourceSets.commonMain.dependencies {
        implementation(libs.multiplatform.navigation3.ui)
        implementation(project(":navigation:contract"))
        implementation(project(":features:watchlists:contract"))
    }
}

compose.resources {
    packageOfResClass = "com.jcotters.features.watchlists.resources"
    publicResClass = true
}

plugins { id("movers.multiplatform.compose") }

kotlin {
    android { namespace = "com.jcotters.styleguide" }
    sourceSets.commonMain.dependencies {
        implementation(libs.multiplatform.navigation3.ui)
        implementation(libs.multiplatform.lifecycle.compose)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.koin.core)
        implementation(project(":designsystem"))
        implementation(project(":navigation:contract"))
        implementation(project(":features:styleguide:contract"))
    }
}

plugins {
    id("movers.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    android { androidResources.enable = true }
    sourceSets.commonMain.dependencies {
        implementation(catalog.findLibrary("multiplatform-runtime").get())
        implementation(catalog.findLibrary("multiplatform-foundation").get())
        implementation(catalog.findLibrary("multiplatform-material3").get())
        implementation(catalog.findLibrary("multiplatform-resources").get())
        implementation(catalog.findLibrary("multiplatform-preview").get())
    }
}

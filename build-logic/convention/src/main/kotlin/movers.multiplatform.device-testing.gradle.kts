plugins { id("movers.multiplatform.library") }

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    android {
        androidResources.enable = true
        withDeviceTest { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    }
    sourceSets.named("androidDeviceTest") {
        dependencies {
            implementation(catalog.findLibrary("androidx-junit").get())
            implementation(catalog.findLibrary("androidx-runner").get())
            implementation(catalog.findLibrary("kotlinx-coroutines-test").get())
            implementation(catalog.findLibrary("androidx-sqlite-bundled").get())
        }
    }
}

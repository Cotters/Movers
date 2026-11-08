plugins { id("movers.multiplatform.library") }
kotlin {
    android {
        namespace = "com.cotters.data.network"
        compilations.withType<com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation>().configureEach {
            isReturnDefaultValues = true
        }
    }
    sourceSets.commonMain.dependencies { api(libs.kotlinx.coroutines.core) }
    sourceSets.named("androidHostTest") { kotlin.srcDir("src/test/java") }
}

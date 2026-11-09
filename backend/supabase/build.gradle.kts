plugins {
    id("movers.multiplatform.library")
    id("movers.kotlin.serialization")
}
kotlin {
    android { namespace = "com.jcotters.backend.supabase" }
    sourceSets.commonMain.dependencies {
        implementation(project(":movie:contract"))
        implementation(project(":profile:contract"))
        implementation(libs.supabase.auth)
        implementation(libs.supabase.postgrest)
        implementation(libs.supabase.realtime)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.kotlinx.coroutines.core)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.ktor.client.okhttp)
        // SDK 3.9 requests Lifecycle 2.11; keep its Android runtime aligned with the shared app.
        implementation(libs.androidx.lifecycle.process.get().toString()) { version { strictly(libs.versions.lifecycleShared.get()) } }
    }
    sourceSets.iosMain.dependencies { implementation(libs.ktor.client.darwin) }
}

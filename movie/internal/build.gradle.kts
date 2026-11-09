plugins {
    id("movers.multiplatform.library")
    id("movers.multiplatform.device-testing")
    id("movers.kotlin.serialization")
}

kotlin {
    android { namespace = "com.jcotters.movie.internal" }
    sourceSets.named("androidHostTest") { kotlin.srcDir("src/test/java") }
    sourceSets.named("androidDeviceTest") { kotlin.srcDir("src/androidTest/java") }
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.koin.core)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.json)
        implementation(libs.androidx.room)
        implementation(libs.androidx.room.paging)
        implementation(project(":database"))
        implementation(project(":profile:contract"))
        implementation(project(":data:network"))
        implementation(project(":movie:contract"))
        implementation(libs.kotlin.serialization.json)
    }
    sourceSets.commonTest.dependencies { implementation(libs.ktor.client.mock) }
}

plugins {
    id("movers.multiplatform.library")
    id("movers.multiplatform.ksp")
}

kotlin {
    android { namespace = "com.jcotters.database" }
    sourceSets.named("androidHostTest") { kotlin.srcDir("src/test/java") }
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.coroutines.core)
        api(libs.androidx.room)
        implementation(libs.androidx.room.paging)
        implementation(libs.androidx.sqlite.bundled)
        implementation(libs.kotlin.serialization.json)
        api(libs.androidx.paging.common)
    }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

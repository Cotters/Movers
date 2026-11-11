plugins {
    id("movers.multiplatform.compose")
    id("movers.kotlin.serialization")
    id("movers.multiplatform.device-testing")
    id("movers.multiplatform.ui-testing")
}

kotlin {
    android {
        namespace = "com.jcotters.shared"
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "MoversShared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.multiplatform.navigation3.ui)
            implementation(libs.multiplatform.lifecycle.compose)
            implementation(libs.multiplatform.lifecycle.navigation3)
            implementation(libs.kotlin.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.androidx.room)
            implementation(project(":features:catalogue"))
            implementation(project(":features:catalogue:contract"))
            implementation(project(":features:details"))
            implementation(project(":features:details:contract"))
            implementation(project(":features:watchlists"))
            implementation(project(":features:watchlists:contract"))
            implementation(project(":features:search"))
            implementation(project(":features:search:contract"))
            implementation(project(":features:styleguide"))
            implementation(project(":features:styleguide:contract"))
            implementation(project(":profile"))
            implementation(project(":profile:contract"))
            implementation(project(":navigation:contract"))
            implementation(project(":presentation"))
            implementation(project(":movie:internal"))
            implementation(project(":movie:contract"))
            implementation(project(":database"))
            implementation(project(":backend:supabase"))
            implementation(project(":data:network"))
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        named("androidHostTest") {
            resources.srcDir("../database/schemas")
            dependencies {
                implementation(libs.ktor.client.mock)
                implementation(libs.androidx.sqlite.framework)
            }
        }
        named("androidDeviceTest") {
            dependencies {
                implementation(project.dependencies.platform(libs.androidx.compose.bom))
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.coil.compose)
                implementation(libs.androidx.paging.compose)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.jcotters.movers.resources"
    publicResClass = true
}

plugins {
    id("movers.android.library")
    id("movers.android.compose")
    id("movers.android.hilt")
    id("movers.android.unit-testing")
}

android {
    namespace = "com.jcotters.profile"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":features:details:contract"))

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.viewmodel.compose)
    implementation(project(":navigation:contract"))
    implementation(project(":profile:contract"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.retrofit.gson)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(project(path = ":auth:contract"))
    implementation(project(path = ":database"))
    implementation(project(path = ":presentation"))
    implementation(project(path = ":movie:contract"))

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

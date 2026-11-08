plugins {
    id("movers.android.application")
    id("movers.android.compose")
    id("movers.android.hilt")
    id("movers.kotlin.serialization")
    id("movers.android.unit-testing")
}

android {
    namespace = "com.jcotters.movers"

    defaultConfig {
        applicationId = "com.jcotters.movers"

        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.viewmodel.compose)
    implementation(project(":navigation:contract"))
    implementation(project(":auth"))
    implementation(project(":features:watchlists"))
    implementation(project(":features:catalogue:contract"))
    implementation(project(":features:details:contract"))
    implementation(project(":features:watchlists:contract"))
    implementation(project(":profile:contract"))
    implementation(project(":auth:contract"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.paging.compose)

    implementation(project(path = ":presentation"))
    implementation(project(path = ":data:network"))
    implementation(project(path = ":movie:contract"))
    implementation(project(path = ":movie:internal"))
    implementation(project(path = ":features:catalogue"))
    implementation(project(path = ":features:details"))
    implementation(project(path = ":profile"))

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

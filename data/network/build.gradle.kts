plugins {
    id("movers.android.library")
    id("movers.android.hilt")
    id("movers.android.unit-testing")
}

android {
    namespace = "com.cotters.data.network"

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}

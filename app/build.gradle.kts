import java.util.Properties

plugins {
    id("movers.android.application")
    id("movers.android.compose")
    id("movers.android.unit-testing")
}
val backendProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun backendString(name: String): String = "\"" + backendProperties.getProperty(name, "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
android {
    buildFeatures { buildConfig = true }
    namespace = "com.jcotters.movers"
    defaultConfig {
        applicationId = "com.jcotters.movers"
        versionCode = 2
        versionName = "2.0"
        buildConfigField("String", "SUPABASE_URL", backendString("supabase.url"))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", backendString("supabase.publishableKey"))
    }
    buildTypes.getByName("release") {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

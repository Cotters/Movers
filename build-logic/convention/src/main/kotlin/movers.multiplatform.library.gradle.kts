import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    android {
        compileSdk = catalog.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk = catalog.findVersion("android-minSdk").get().requiredVersion.toInt()
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        withHostTest { }
    }
    iosArm64()
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(catalog.findLibrary("kotlinx-coroutines-test").get())
        }
        named("androidHostTest") {
            dependencies {
                implementation(catalog.findLibrary("junit").get())
                implementation(catalog.findLibrary("mockk").get())
            }
        }
    }
}

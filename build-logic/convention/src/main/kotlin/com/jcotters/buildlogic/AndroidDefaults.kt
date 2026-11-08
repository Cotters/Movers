package com.jcotters.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureAndroidDefaults(android: CommonExtension) {
    android.apply {
        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        defaultConfig.apply {
            minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

internal fun Project.withAndroid(action: Project.(CommonExtension) -> Unit) {
    listOf("com.android.application", "com.android.library", "com.android.test").forEach { id ->
        pluginManager.withPlugin(id) {
            action(extensions.getByType<CommonExtension>())
        }
    }
}

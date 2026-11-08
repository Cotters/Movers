package com.jcotters.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        withAndroid { android ->
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            android.buildFeatures.compose = true
            dependencies {
                add("implementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
            }
            if (!pluginManager.hasPlugin("com.android.test")) {
                dependencies {
                    add("androidTestImplementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                }
            }
        }
    }
}

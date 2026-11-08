package com.jcotters.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidUnitTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        listOf("com.android.application", "com.android.library").forEach { id ->
            pluginManager.withPlugin(id) {
                dependencies {
                    add("testImplementation", libs.findLibrary("junit").get())
                    add("testImplementation", libs.findLibrary("mockk").get())
                    add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
                }
            }
        }
    }
}

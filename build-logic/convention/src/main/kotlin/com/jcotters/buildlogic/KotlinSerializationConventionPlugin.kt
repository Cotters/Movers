package com.jcotters.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinSerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    }
}

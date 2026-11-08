package com.jcotters.buildlogic

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ConventionPluginsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `plain library has Android defaults without optional tooling and allows overrides`() {
        val project = fixture("contract")
        project.module("contract", """
            plugins { id 'movers.android.library' }
            android { namespace = 'fixture.contract' }

            assert android.compileSdk == 37
            assert android.defaultConfig.minSdk == 28
            androidComponents.onVariants(androidComponents.selector().all()) {
                assert android.compileOptions.sourceCompatibility.toString() == '17'
                assert android.compileOptions.targetCompatibility.toString() == '17'
            }
            assert android.defaultConfig.testInstrumentationRunner == 'androidx.test.runner.AndroidJUnitRunner'
            assert !pluginManager.hasPlugin('org.jetbrains.kotlin.android')
            assert !pluginManager.hasPlugin('com.google.dagger.hilt.android')
            assert !pluginManager.hasPlugin('com.google.devtools.ksp')
            assert !pluginManager.hasPlugin('org.jetbrains.kotlin.plugin.compose')
            assert !android.buildFeatures.compose
            assert configurations.implementation.dependencies.empty
            assert configurations.testImplementation.dependencies.empty

            android { defaultConfig { minSdk = 29 } }
            assert android.defaultConfig.minSdk == 29
        """)
        project.run("help")
    }

    @Test
    fun `application and library opt into Compose Hilt serialization and host tests`() {
        val project = fixture("app", "feature")
        project.module("app", """
            plugins {
                id 'movers.android.application'
                id 'movers.android.compose'
                id 'movers.android.hilt'
                id 'movers.kotlin.serialization'
                id 'movers.android.unit-testing'
            }
            android {
                namespace = 'fixture.app'
                defaultConfig { applicationId = 'fixture.app' }
            }
            assert android.defaultConfig.targetSdk == 36
            android { defaultConfig { targetSdk = 35 } }
            assert android.defaultConfig.targetSdk == 35
            $optionalAssertions
        """)
        // Add-ons can be declared before the base plugin without losing configuration.
        project.module("feature", """
            plugins {
                id 'movers.android.compose'
                id 'movers.android.hilt'
                id 'movers.android.unit-testing'
                id 'movers.android.library'
                id 'movers.kotlin.serialization'
            }
            android { namespace = 'fixture.feature' }
            $optionalAssertions
        """)
        project.run("help")
    }

    @Test
    fun `standalone Android tests use their own base and Compose configuration`() {
        val project = fixture("app", "device")
        project.module("app", """
            plugins { id 'movers.android.application' }
            android {
                namespace = 'fixture.app'
                defaultConfig { applicationId = 'fixture.app' }
            }
        """)
        project.module("device", """
            plugins {
                id 'movers.android.test'
                id 'movers.android.compose'
            }
            android {
                namespace = 'fixture.device'
                targetProjectPath = ':app'
            }
            assert android.compileSdk == 37
            assert android.defaultConfig.minSdk == 28
            androidComponents.onVariants(androidComponents.selector().all()) {
                assert android.compileOptions.targetCompatibility.toString() == '17'
            }
            assert android.buildFeatures.compose
            assert configurations.implementation.dependencies.any { it.name == 'compose-bom' }
            assert !pluginManager.hasPlugin('com.google.dagger.hilt.android')
        """)
        project.run("help")
    }

    @Test
    fun `optional conventions permit configuration cache reuse`() {
        val project = fixture("app")
        project.module("app", """
            plugins {
                id 'movers.android.application'
                id 'movers.android.compose'
                id 'movers.android.hilt'
                id 'movers.kotlin.serialization'
                id 'movers.android.unit-testing'
            }
            android {
                namespace = 'fixture.app'
                defaultConfig { applicationId = 'fixture.app' }
            }
        """)
        project.run("help", "--configuration-cache", "--configuration-cache-problems=fail")
        val second = project.run("help", "--configuration-cache", "--configuration-cache-problems=fail")
        assertTrue(second.output, second.output.contains("Reusing configuration cache."))
    }

    private fun fixture(vararg modules: String): File {
        val directory = temporaryFolder.newFolder()
        File(System.getProperty("movers.versionCatalog")).copyTo(File(directory, "libs.versions.toml"))
        File(directory, "settings.gradle").writeText("""
            dependencyResolutionManagement {
                repositories { google(); mavenCentral() }
                versionCatalogs { libs { from(files('libs.versions.toml')) } }
            }
            rootProject.name = 'convention-fixture'
            include ${modules.joinToString(", ") { "':$it'" }}
        """.trimIndent())
        File(directory, "gradle.properties").writeText("android.useAndroidX=true\n")
        return directory
    }

    private fun File.module(name: String, script: String) {
        File(this, name).mkdirs()
        File(this, "$name/build.gradle").writeText(script.trimIndent())
    }

    private fun File.run(vararg arguments: String) = GradleRunner.create()
        .withProjectDir(this)
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace", "--console=plain", "--max-workers=2")
        .build()

    private val optionalAssertions = """
        assert android.buildFeatures.compose
        assert pluginManager.hasPlugin('com.google.devtools.ksp')
        assert pluginManager.hasPlugin('com.google.dagger.hilt.android')
        assert pluginManager.hasPlugin('org.jetbrains.kotlin.plugin.serialization')
        assert configurations.implementation.dependencies.any { it.name == 'compose-bom' }
        assert configurations.implementation.dependencies.any { it.name == 'ui-tooling-preview' }
        assert configurations.debugImplementation.dependencies.any { it.name == 'ui-tooling' }
        assert configurations.androidTestImplementation.dependencies.any { it.name == 'compose-bom' }
        assert configurations.implementation.dependencies.any { it.name == 'hilt-android' }
        assert configurations.ksp.dependencies.any { it.name == 'hilt-android-compiler' }
        assert configurations.testImplementation.dependencies*.name.containsAll(['junit', 'mockk', 'kotlinx-coroutines-test'])
    """.trimIndent()
}

plugins {
    `kotlin-dsl`
}

group = "com.jcotters.buildlogic"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.compose.gradlePlugin)
    implementation(libs.kotlin.serialization.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.multiplatform.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    testImplementation(gradleTestKit())
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "movers.android.application"
            implementationClass = "com.jcotters.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "movers.android.library"
            implementationClass = "com.jcotters.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidTest") {
            id = "movers.android.test"
            implementationClass = "com.jcotters.buildlogic.AndroidTestConventionPlugin"
        }
        register("androidCompose") {
            id = "movers.android.compose"
            implementationClass = "com.jcotters.buildlogic.AndroidComposeConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "movers.kotlin.serialization"
            implementationClass = "com.jcotters.buildlogic.KotlinSerializationConventionPlugin"
        }
        register("androidUnitTesting") {
            id = "movers.android.unit-testing"
            implementationClass = "com.jcotters.buildlogic.AndroidUnitTestingConventionPlugin"
        }
    }
}

tasks.withType<ValidatePlugins>().configureEach {
    enableStricterValidation = true
    failOnWarning = true
}

tasks.test {
    inputs.file("../../gradle/libs.versions.toml")
    systemProperty("movers.versionCatalog", file("../../gradle/libs.versions.toml").absolutePath)
}

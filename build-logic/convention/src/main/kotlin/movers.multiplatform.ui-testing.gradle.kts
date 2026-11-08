import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation

plugins { id("movers.multiplatform.compose") }

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    android {
        compilations.withType<KotlinMultiplatformAndroidHostTestCompilation>().configureEach {
            isIncludeAndroidResources = true
        }
    }
    sourceSets.named("androidHostTest") {
        dependencies {
            implementation(project.dependencies.platform(catalog.findLibrary("androidx-compose-bom").get()))
            implementation(catalog.findLibrary("androidx-compose-ui-test-junit4").get())
            implementation(catalog.findLibrary("androidx-compose-ui-test-manifest").get())
            implementation(catalog.findLibrary("robolectric").get())
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    )
}

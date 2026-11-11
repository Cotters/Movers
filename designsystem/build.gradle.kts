plugins { id("movers.multiplatform.compose") }

kotlin {
    android { namespace = "com.jcotters.designsystem" }
    sourceSets.commonMain.dependencies {
        implementation(libs.unstyled.button)
        implementation(libs.unstyled.text.field)
        implementation(libs.unstyled.dialog)
        implementation(libs.unstyled.dropdown.menu)
        implementation(libs.unstyled.modal.bottom.sheet)
    }
}

compose.resources {
    packageOfResClass = "com.jcotters.designsystem.resources"
    publicResClass = true
}

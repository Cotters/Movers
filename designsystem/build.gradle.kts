plugins { id("movers.multiplatform.compose") }

kotlin {
    android { namespace = "com.jcotters.designsystem" }
}

compose.resources {
    packageOfResClass = "com.jcotters.designsystem.resources"
    publicResClass = true
}

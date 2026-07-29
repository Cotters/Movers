pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Movers"
include(":app")
include(":movie")
include(":auth")
include(":database")
include(":profile")
include(":tests")
include(":movie:contract")
include(":movie:internal")
include(":features")
include(":features:catalogue")
include(":features:details")
include(":presentation")

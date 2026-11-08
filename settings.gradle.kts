pluginManagement {
    includeBuild("build-logic")
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
include(":data:network")

include(":features:catalogue:contract")
include(":features:details:contract")
include(":features:watchlists:contract")
include(":profile:contract")
include(":auth:contract")
include(":navigation:contract")
include(":features:watchlists")

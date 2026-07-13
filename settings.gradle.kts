pluginManagement {
    // Convention plugins live in a separate composite build so that
    // Gradle configuration is shared across every module without duplication.
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sylo"

// ----- Application -----
include(":app")

// ----- Core (shared infrastructure; features depend only on these) -----
include(":core:core-common")
include(":core:core-ui")
include(":core:core-navigation")
include(":core:core-network")
include(":core:core-database")
include(":core:core-security")

// ----- Features (must NOT depend on each other) -----
include(":feature:feature-auth")
include(":feature:feature-dashboard")
include(":feature:feature-transactions")
include(":feature:feature-voice")
include(":feature:feature-settings")

plugins {
    `kotlin-dsl`
}

group = "com.sylo.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // These plugins are on the classpath so the convention plugins below can call
    // their APIs (LibraryExtension, KotlinAndroidProjectExtension, etc.). They are
    // `compileOnly` because the real plugins are applied by the consuming modules.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

// Register each convention plugin under a stable id used from the version catalog.
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "sylo.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "sylo.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "sylo.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "sylo.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "sylo.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidRoom") {
            id = "sylo.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
    }
}

plugins {
    alias(libs.plugins.sylo.android.application)
    alias(libs.plugins.sylo.android.compose)
    alias(libs.plugins.sylo.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sylo.com"

    defaultConfig {
        applicationId = "com.sylo.com"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        // Required for the per-flavor resValue("string", "app_name", …) below (AGP 9
        // disables this by default, same as buildConfig).
        resValues = true
    }

    // Product flavors: one build per backend environment. Each carries its own
    // applicationId suffix (so dev/staging/prod can be installed side by side),
    // API base URL + logging switch (exposed via BuildConfig), and app name.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.sylo.example.com/\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            resValue("string", "app_name", "Sylo Dev")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "API_BASE_URL", "\"https://staging-api.sylo.example.com/\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            resValue("string", "app_name", "Sylo Staging")
        }
        create("prod") {
            dimension = "environment"
            // No suffix — production keeps the canonical applicationId/name.
            buildConfigField("String", "API_BASE_URL", "\"https://api.sylo.example.com/\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            resValue("string", "app_name", "Sylo")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    // --- Core: the app assembles the whole graph, so it sees every module ---
    implementation(project(":core:core-common"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-navigation"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-security"))

    // --- Features: only :app knows about all of them (to build the nav graph) ---
    implementation(project(":feature:feature-auth"))
    implementation(project(":feature:feature-dashboard"))
    implementation(project(":feature:feature-transactions"))
    implementation(project(":feature:feature-voice"))
    implementation(project(":feature:feature-settings"))

    // --- Compose / AndroidX runtime ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    // Navigation 3: the app owns the NavDisplay (ui) and back-stack state (runtime),
    // plus the ViewModel add-on so per-entry ViewModels are scoped correctly.
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.timber)

    // Background bank-SMS capture: WorkManager drives the periodic rescan, and the
    // HiltWorkerFactory (hilt-work) lets the @HiltWorker be constructor-injected.
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Home-screen widget: Glance composes the widget UI with Compose-like APIs,
    // rendered through RemoteViews under the hood.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Dagger/Hilt generated code references these annotations at compile time.
    compileOnly(libs.error.prone.annotations)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

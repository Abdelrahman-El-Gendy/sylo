plugins {
    // Pulls in the library + Compose + Hilt conventions and the sanctioned core
    // dependencies (core-common, core-ui, core-navigation) automatically.
    alias(libs.plugins.sylo.android.feature)
}

android {
    namespace = "com.sylo.feature.auth"
}

dependencies {
    // Auth is the one feature that legitimately needs the security primitives
    // (session, biometric, PIN). Still a :core dependency — never another feature.
    implementation(project(":core:core-security"))
    // First-run onboarding persists a "completed" flag via user preferences.
    implementation(project(":core:core-database"))

    implementation(libs.androidx.compose.material.icons.extended)
}

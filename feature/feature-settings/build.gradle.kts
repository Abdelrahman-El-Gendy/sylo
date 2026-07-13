plugins {
    alias(libs.plugins.sylo.android.feature)
}

android {
    namespace = "com.sylo.feature.settings"
}

dependencies {
    // Settings manages security options (change PIN, logout), so it uses core-security.
    implementation(project(":core:core-security"))
    // Profile header stats are computed from real transactions.
    implementation(project(":core:core-database"))
    // Profile photo uses the Photo Picker.
    implementation(libs.androidx.activity.compose)
}

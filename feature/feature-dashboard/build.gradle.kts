plugins {
    alias(libs.plugins.sylo.android.feature)
}

android {
    namespace = "com.sylo.feature.dashboard"
}

dependencies {
    // Recent transactions are read from the encrypted local DB.
    implementation(project(":core:core-database"))
}

plugins {
    alias(libs.plugins.sylo.android.library)
    alias(libs.plugins.sylo.android.hilt)
    alias(libs.plugins.sylo.android.room)
}

android {
    namespace = "com.sylo.core.database"
}

dependencies {
    implementation(project(":core:core-common"))

    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}

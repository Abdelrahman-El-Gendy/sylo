plugins {
    alias(libs.plugins.sylo.android.feature)
}

android {
    namespace = "com.sylo.feature.transactions"
}

dependencies {
    // History reads from — and Add Expense writes to — the encrypted local DB.
    implementation(project(":core:core-database"))
    // Add Expense uses the Photo Picker to attach a receipt image.
    implementation(libs.androidx.activity.compose)
}

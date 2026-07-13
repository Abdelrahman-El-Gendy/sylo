import com.sylo.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * The convention every :feature module applies. It composes the library, Compose
 * and Hilt conventions, then wires the *only* dependencies a feature is allowed to
 * have: the shared :core modules plus navigation/viewmodel plumbing.
 *
 * Enforces the architecture rule: features depend on :core, never on each other.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("sylo.android.library")
            apply("sylo.android.compose")
            apply("sylo.android.hilt")
        }

        dependencies {
            // Shared infrastructure — the sanctioned dependencies for a feature.
            add("implementation", project(":core:core-common"))
            add("implementation", project(":core:core-ui"))
            add("implementation", project(":core:core-navigation"))

            // Navigation 3 + ViewModel plumbing used by essentially every feature.
            // Features declare their destinations as EntryProviderScope extensions, so
            // they only need the Nav3 runtime (NavKey/entry), not the UI (NavDisplay).
            add("implementation", libs.findLibrary("androidx-navigation3-runtime").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("hilt-navigation-compose").get())
            add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
        }
    }
}

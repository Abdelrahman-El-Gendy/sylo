import com.android.build.api.dsl.CommonExtension
import com.sylo.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Enables Jetpack Compose for a module that already applies either the library or
 * application convention. Applies the Kotlin Compose compiler plugin and wires the
 * Compose BOM + core dependencies via [configureAndroidCompose].
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        // Works for both application and library modules.
        val extension = extensions.getByType<CommonExtension>()
        configureAndroidCompose(extension)
    }
}

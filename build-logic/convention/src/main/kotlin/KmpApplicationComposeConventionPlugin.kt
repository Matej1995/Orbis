
import com.packeta.orbis.convention.configureAndroidTarget
import com.packeta.orbis.convention.configureIosTargets
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpApplicationComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.packeta.orbis.convention.android.application.compose")
                apply("org.jetbrains.kotlin.multiplatform")
            }

            configureAndroidTarget()
            configureIosTargets()
        }
    }
}

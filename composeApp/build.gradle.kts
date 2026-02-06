plugins {
    alias(libs.plugins.convention.kmp.application.compose)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            // Android Compose
            implementation(libs.androidx.material3)
            implementation(libs.androidx.ui)

            // AndroidX Core
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.activity.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.core.splashscreen)

            // Lifecycle
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Koin
            implementation(libs.bundles.koin.android)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Permissions
            implementation(libs.moko.permissions.compose)
        }

        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.domain)
            implementation(projects.core.presentation)
        }
    }
}
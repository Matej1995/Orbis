plugins {
    alias(libs.plugins.convention.kmp.library.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.jetbrains.lifecycle.viewmodel)
        }

        androidMain.dependencies {
            implementation(libs.androidx.material3)
            implementation(libs.androidx.ui)
        }
    }
}
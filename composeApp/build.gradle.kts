import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.contentnegotiation)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
        }

        val desktopTest by getting {
            dependencies {
                implementation(libs.mockk)
                implementation(libs.junit)
                implementation(libs.kotlin.test.junit)
            }
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            runtimeOnly(libs.slf4j.simple)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.hitlist.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.hitlist"
            packageVersion = "1.0.0"
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    if (name == "desktopRun") {
        mainClass.set("com.hitlist.MainKt")
    }
}

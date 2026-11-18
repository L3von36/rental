import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.compose") version "1.9.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

group = "com.loypos.desktop"

val rawVer = (findProperty("appVersion") as? String)
    ?.trim()
    ?.removePrefix("v")
    ?.substringBefore("-")
    ?: "1.0.0"
val verParts = rawVer.split(".").mapNotNull { it.toIntOrNull() }
val cleanPackageVersion = when {
    verParts.size >= 3 -> "${verParts[0].coerceAtLeast(1)}.${verParts[1]}.${verParts[2]}"
    verParts.size == 2 -> "${verParts[0].coerceAtLeast(1)}.${verParts[1]}.0"
    verParts.size == 1 -> "${verParts[0].coerceAtLeast(1)}.0.0"
    else -> "1.0.0"
}
version = cleanPackageVersion

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.loypos.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb,
            )
            packageName = "LoyPOS-Rental"
            packageVersion = cleanPackageVersion
            description = "LoyPOS Event Equipment Rental Desktop"
            vendor = "LoyPOS"
            includeAllModules = true

            macOS {
                bundleID = "com.aistudio.loypos.desktop"
            }
            windows {
                menuGroup = "LoyPOS Rental"
                perUserInstall = true
                upgradeUuid = "9a3b614e-4b78-4f66-924c-64d7254df801"
            }
            linux {
                packageName = "loypos-rental"
            }
        }
    }
}

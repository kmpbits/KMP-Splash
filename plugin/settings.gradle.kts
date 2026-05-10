rootProject.name = "kmp-splash-plugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Load version from root gradle.properties so the composite build stays in sync
val rootProps = java.util.Properties()
file("../gradle.properties").inputStream().use { rootProps.load(it) }
gradle.beforeProject {
    rootProps.forEach { key, value -> extra[key.toString()] = value }
}

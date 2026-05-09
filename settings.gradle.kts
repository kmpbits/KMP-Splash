rootProject.name = "kmp-splash"

// The plugin is developed in-repo — wire it as a composite build so the
// sample can apply it without publishing to Maven Local first.
pluginManagement {
    includeBuild("plugin")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

include(":runtime")
include(":sample:composeApp")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

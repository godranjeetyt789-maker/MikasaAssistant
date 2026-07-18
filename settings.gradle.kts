pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Uncomment if you add the Porcupine wake-word SDK (see README > Wake Word):
        // maven { url = uri("https://maven.picovoice.ai/") }
    }
}

rootProject.name = "MikasaAssistant"
include(":app")

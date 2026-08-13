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
    }
}

rootProject.name = "LocalAI"
include(":shared")
include(":androidApp")
include(":llamaAndroid")
project(":androidApp").projectDir = file("android/app")
project(":llamaAndroid").projectDir = file("third_party/llama.cpp/examples/llama.android/lib")

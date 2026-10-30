pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); maven("https://jitpack.io") }
}
rootProject.name = "GBA4KUltimate"
include(":app")

include(":libretrodroid")
project(":libretrodroid").projectDir = file("external/libretrodroid/libretrodroid")

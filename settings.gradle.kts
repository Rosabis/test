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
        maven("https://maven.miui.com/nexus/content/repositories/public/")
        maven("https://maven.aliyun.com/repository/public")
    }
}

rootProject.name = "ApkRenamer"
include(":app")

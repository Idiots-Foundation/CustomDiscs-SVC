@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.subkek.space/maven-public") { name = "subkek" }
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
        maven("https://maven.maxhenkel.de/repository/public") { name = "maxhenkel" }
        maven("https://maven.lavalink.dev/snapshots") { name = "lavalink-snapshots" }
        maven("https://maven.lavalink.dev/releases") { name = "lavalink-releases" }
        maven("https://jitpack.io") { name = "jitpack" }
    }
}

rootProject.name = "CustomDiscs"
include(":api")
include(":api-test")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "flip-sync-be"
include("common")
include("flip-sync-server")
include("flip-sync-db")

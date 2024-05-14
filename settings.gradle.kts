plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "delta-coverage-gradle-plugin"
include("delta-coverage-core")
include("delta-coverage-gradle")

dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            from(files("gradle/deps.versions.toml"))
        }
    }
}

buildCache {
    local {
        isEnabled = true
    }
}

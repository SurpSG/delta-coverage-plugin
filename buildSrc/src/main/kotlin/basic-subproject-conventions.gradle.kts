plugins {
    kotlin("jvm")
    `maven-publish`

    id("io.gitlab.arturbosch.detekt")
    id("unit-tests-conventions")

    id("basic-coverage-conventions")
}

val targetJvmVersion = JavaLanguageVersion.of(11)
kotlin {
    jvmToolchain {
        languageVersion.set(targetJvmVersion)
    }
}

java {
    toolchain {
        languageVersion.set(targetJvmVersion)
    }
}

repositories {
    mavenCentral()
}

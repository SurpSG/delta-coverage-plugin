plugins {
    `basic-subproject-conventions`
    id("org.jetbrains.kotlinx.kover") version "0.7.0-Beta"
}

publishing {
    publications {
        create<MavenPublication>("jacocoFilteringExtension") {
            from(components["java"])
        }
    }
}

dependencies {
    implementation("org.jetbrains.intellij.deps:intellij-coverage-reporter:1.0.716")
    implementation(deps.jgit)
    implementation(deps.jacocoCore)
    implementation(deps.jacocoReport)
    implementation(deps.httpClient)
}

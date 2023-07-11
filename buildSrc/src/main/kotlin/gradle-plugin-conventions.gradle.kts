plugins {
    id("basic-subproject-conventions")

    id("java-gradle-plugin")
    id("functional-tests-conventions")
    id("pl.droidsonroids.jacoco.testkit")
    `java-test-fixtures`

    id("com.gradle.plugin-publish")
}

val functionalTestSuite: JvmTestSuite = testing.suites.getByName("functionalTest") as JvmTestSuite
configure<GradlePluginDevelopmentExtension> {
    testSourceSets(
        functionalTestSuite.sources,
        project.extensions.getByType(JavaPluginExtension::class).sourceSets.getByName("testFixtures")
    )
}

configure<pl.droidsonroids.gradle.jacoco.testkit.JacocoTestKitExtension> {
    applyTo("functionalTestRuntimeOnly", tasks.named("functionalTest"))
}

plugins {
    base
    id("delta-coverage-conventions")
    `test-report-aggregation`
    `coverage-aggregate-conventions`
    alias(deps.plugins.depUpdatesPlugin)
}

repositories {
    mavenCentral()
}

dependencies {
    allprojects {
        testReportAggregation(this)
    }
}

tasks.named("check") {
    dependsOn(
        provider {
            subprojects.map { it.tasks.named("check") }
        }
    )
}

tasks.named("processTestResources") {
    println(this::class.java.simpleName)
}

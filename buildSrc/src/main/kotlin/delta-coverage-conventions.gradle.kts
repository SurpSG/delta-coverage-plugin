import io.github.surpsg.deltacoverage.CoverageEngine

plugins {
    base
    id("basic-coverage-conventions")
    id("io.github.surpsg.delta-coverage")
}

val isGithub = project.hasProperty("github")

deltaCoverageReport {
    coverage.engine = CoverageEngine.INTELLIJ

    diffSource {
        git.diffBase = project.properties["diffBase"]?.toString() ?: "refs/remotes/origin/main"
    }

    coverageBinaryFiles = if (isGithub) {
        fileTree("tests-artifacts/") { include("**/*.ic") }
    } else {
        fileTree(".") { include("*/build/**/*.ic") }
    }

    reports {
        html = true
        xml = true
        fullCoverageReport = true
    }

    violationRules.failIfCoverageLessThan(0.9)
}

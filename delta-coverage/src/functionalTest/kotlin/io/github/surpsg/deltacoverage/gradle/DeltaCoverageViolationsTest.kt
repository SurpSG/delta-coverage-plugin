package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.gradle.DeltaCoveragePlugin.Companion.DELTA_COVERAGE_TASK
import io.github.surpsg.deltacoverage.gradle.resources.toUnixAbsolutePath
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class DeltaCoverageViolationsTest : BaseDeltaCoverageTest() {

    companion object {
        const val TEST_PROJECT_RESOURCE_NAME = "single-module-test-project"
    }

    override fun buildTestConfiguration() = TestConfiguration(
        TEST_PROJECT_RESOURCE_NAME,
        "build.gradle",
        "test.diff.file"
    )

    @BeforeEach
    fun setup() {
        initializeGradleTest()
    }

    @Test
    fun `delta-coverage should validate coverage and fail without report creation`() {
        // setup
        val baseReportDir = "build/custom/reports/dir/jacoco/"
        buildFile.appendText(
            """
            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
                reportConfiguration.baseReportDir.set('$baseReportDir')
                violationRules {
                    failIfCoverageLessThan 1.0
                    failOnViolation.set(true)
                }
            }
        """.trimIndent()
        )

        // run
        val result = gradleRunner.runTaskAndFail(DELTA_COVERAGE_TASK)

        // assert
        result.assertDeltaCoverageStatusEqualsTo(FAILED)
            .assertOutputContainsStrings("Fail on violations: true. Found violations: 3")
        assertThat(
            rootProjectDir.resolve(baseReportDir).resolve("deltaCoverage")
        ).doesNotExist()
    }

    @Test
    fun `delta-coverage should fail on violation and generate html report`() {
        // setup
        val absolutePathBaseReportDir = rootProjectDir
            .resolve("build/absolute/path/reports/jacoco/")
            .toUnixAbsolutePath()

        buildFile.appendText(
            """

            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
                reports {
                    html.set(true)
                    baseReportDir.set('$absolutePathBaseReportDir')
                }
                violationRules {
                    minBranches.set(0.6d)
                    minLines.set(0.7d)
                    minInstructions.set(0.8d)
                    failOnViolation.set(true)
                }
            }
        """.trimIndent()
        )

        // run
        val result = gradleRunner.runTaskAndFail(DELTA_COVERAGE_TASK)

        // assert
        result.assertDeltaCoverageStatusEqualsTo(FAILED)
            .assertOutputContainsStrings(
                "instructions covered ratio is 0.5, but expected minimum is 0.8",
                "branches covered ratio is 0.5, but expected minimum is 0.6",
                "lines covered ratio is 0.6, but expected minimum is 0.7"
            )

        val deltaCoverageDir = Paths.get(absolutePathBaseReportDir, "deltaCoverage", "html").toFile()
        assertThat(deltaCoverageDir.list())
            .containsExactlyInAnyOrder(
                *expectedHtmlReportFiles("com.java.test")
            )
    }

    @Test
    fun `delta-coverage should not fail on violation when failOnViolation is false`() {
        // setup
        buildFile.appendText(
            """

            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
                violationRules {
                    failIfCoverageLessThan 1.0d
                    failOnViolation.set(false)
                }
            }
        """.trimIndent()
        )

        // run
        val result = gradleRunner.runTask(DELTA_COVERAGE_TASK)

        // assert
        result.assertDeltaCoverageStatusEqualsTo(SUCCESS)
            .assertOutputContainsStrings("Fail on violations: false. Found violations: 3")
    }

}

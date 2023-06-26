package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.gradle.DeltaCoveragePlugin.Companion.DELTA_COVERAGE_TASK
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ThresholdTest : BaseDeltaCoverageTest() {

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

    @ParameterizedTest
    @CsvSource(
        value = [
            "INSTRUCTION, 18",
            "BRANCH, 7",
            "LINE, 7",
        ]
    )
    fun `delta-coverage should ignore low coverage if entity threshold is not met`(
        coverageEntity: String,
        entityCountThreshold: Int
    ) {
        // GIVEN
        buildFile.appendText(
            """

            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
                
                violationRules {
                    failOnViolation.set(true)
                    
                    rule(io.github.surpsg.deltacoverage.gradle.CoverageEntity.$coverageEntity) {
                        minCoverageRatio.set(1d)
                        entityCountThreshold.set($entityCountThreshold)
                    }
                }
            }
        """.trimIndent()
        )

        // WHEN // THEN
        gradleRunner.runTask(DELTA_COVERAGE_TASK)
            .assertDeltaCoverageStatusEqualsTo(SUCCESS)
            .assertOutputContainsStrings("violation", coverageEntity, "ignored")
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "INSTRUCTION, 17",
            "BRANCH, 6",
            "LINE, 6",
        ]
    )
    fun `delta-coverage should fail build if coverage is low and entity count greater or equal to threshold`(
        coverageEntity: CoverageEntity,
        entityCountThreshold: Int,
    ) {
        // setup
        buildFile.appendText(
            """

            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
                
                violationRules {
                    failOnViolation.set(true)
                    
                    rule(io.github.surpsg.deltacoverage.gradle.CoverageEntity.${coverageEntity.name}) {
                        minCoverageRatio.set(1d)
                        entityCountThreshold.set($entityCountThreshold)
                    }
                }
            }
        """.trimIndent()
        )

        // run
        val result = gradleRunner.runTaskAndFail(DELTA_COVERAGE_TASK)

        // assert
        result.assertDeltaCoverageStatusEqualsTo(FAILED)
    }
}

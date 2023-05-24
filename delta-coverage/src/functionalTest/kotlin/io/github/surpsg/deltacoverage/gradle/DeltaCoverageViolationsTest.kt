package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.gradle.resources.toUnixAbsolutePath
import io.github.surpsg.deltacoverage.gradle.test.GradlePluginTest
import io.github.surpsg.deltacoverage.gradle.test.GradleRunnerInstance
import io.github.surpsg.deltacoverage.gradle.test.ProjectFile
import io.github.surpsg.deltacoverage.gradle.test.RestorableFile
import io.github.surpsg.deltacoverage.gradle.test.RootProjectDir
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import java.nio.file.Paths

@GradlePluginTest(TestProjects.SINGLE_MODULE)
class DeltaCoverageViolationsTest {

    @RootProjectDir
    lateinit var rootProjectDir: File

    @ProjectFile("test.diff.file")
    lateinit var diffFilePath: String

    @ProjectFile("build.gradle")
    lateinit var buildFile: RestorableFile

    @GradleRunnerInstance
    lateinit var gradleRunner: GradleRunner

    @BeforeEach
    fun beforeEach() {
        buildFile.restoreOriginContent()
    }

    @Nested
    inner class ViolationsTest {

        @Test
        fun `delta-coverage should validate coverage and fail without report creation`() {
            // setup
            val baseReportDir = "build/custom/reports/dir/jacoco/"
            buildFile.file.appendText(
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

            // run // assert
            gradleRunner
                .runDeltaCoverageTaskAndFail()
                .assertOutputContainsStrings("Fail on violations: true. Found violations: 3")

            // and assert
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

            buildFile.file.appendText(
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

            // run // assert
            gradleRunner
                .runDeltaCoverageTaskAndFail()
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
            buildFile.file.appendText(
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

            // run // assert
            gradleRunner
                .runDeltaCoverageTask()
                .assertOutputContainsStrings("Fail on violations: false. Found violations: 3")
        }
    }

    @Nested
    inner class CoverageEntityThresholdTest {

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
            buildFile.file.appendText(
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
            gradleRunner
                .runDeltaCoverageTask()
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
            // GIVEN
            buildFile.file.appendText(
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

            // WHEN // THEN
            gradleRunner.runDeltaCoverageTaskAndFail()
        }
    }
}

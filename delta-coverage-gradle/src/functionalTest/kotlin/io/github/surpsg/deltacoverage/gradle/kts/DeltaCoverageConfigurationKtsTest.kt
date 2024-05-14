package io.github.surpsg.deltacoverage.gradle.kts

import io.github.surpsg.deltacoverage.gradle.TestProjects
import io.github.surpsg.deltacoverage.gradle.assertOutputContainsStrings
import io.github.surpsg.deltacoverage.gradle.runDeltaCoverageTask
import io.github.surpsg.deltacoverage.gradle.test.GradlePluginTest
import io.github.surpsg.deltacoverage.gradle.test.GradleRunnerInstance
import io.github.surpsg.deltacoverage.gradle.test.ProjectFile
import io.github.surpsg.deltacoverage.gradle.test.RestorableFile
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@GradlePluginTest(TestProjects.SINGLE_MODULE, kts = true)
class DeltaCoverageConfigurationKtsTest {

    @ProjectFile("test.diff.file")
    lateinit var diffFilePath: String

    @ProjectFile("build.gradle.kts")
    lateinit var buildFile: RestorableFile

    @GradleRunnerInstance
    lateinit var gradleRunner: GradleRunner

    @BeforeEach
    fun beforeEach() {
        buildFile.restoreOriginContent()
    }

    @Nested
    inner class DeltaCoverageViolationsKtsTest {

        @Test
        fun `delta-coverage rules should be configured with single all function`() {
            // GIVEN
            buildFile.file.appendText(
                """
            configure<DeltaCoverageConfiguration> {
                diffSource.file.set("$diffFilePath")
                defaultReportView {
                    violationRules {
                        failOnViolation.set(true)
                        all {
                            entityCountThreshold.set(100)
                            minCoverageRatio.set(1.0)
                        }
                    }
                }
            }
        """.trimIndent()
            )

            // WHEN // THEN
            gradleRunner
                .runDeltaCoverageTask(printLogs = true)
                .assertOutputContainsStrings("violation", "ignored", "INSTRUCTION", "BRANCH", "LINE")
        }

        @Test
        fun `delta-coverage rule should be configured with invoke operator`() {
            // GIVEN
            buildFile.file.appendText(
                """
            configure<DeltaCoverageConfiguration> {
                diffSource.file.set("$diffFilePath")
                defaultReportView {
                    violationRules {
                        failIfCoverageLessThan(1.0)
                        INSTRUCTION.invoke { entityCountThreshold.set(1000) }
                        BRANCH { entityCountThreshold.set(1000) }
                        LINE { entityCountThreshold.set(1000) }
                    }
                }
            }
        """.trimIndent()
            )

            // WHEN // THEN
            gradleRunner
                .runDeltaCoverageTask()
                .assertOutputContainsStrings("violation", "ignored", "INSTRUCTION", "BRANCH", "LINE")
        }
    }
}

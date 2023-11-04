package io.github.surpsg.deltacoverage.gradle.kover

import io.github.surpsg.deltacoverage.gradle.TestProjects
import io.github.surpsg.deltacoverage.gradle.assertOutputContainsStrings
import io.github.surpsg.deltacoverage.gradle.expectedHtmlReportFiles
import io.github.surpsg.deltacoverage.gradle.runDeltaCoverageTask
import io.github.surpsg.deltacoverage.gradle.test.GradlePluginTest
import io.github.surpsg.deltacoverage.gradle.test.GradleRunnerInstance
import io.github.surpsg.deltacoverage.gradle.test.ProjectFile
import io.github.surpsg.deltacoverage.gradle.test.RestorableFile
import io.github.surpsg.deltacoverage.gradle.test.RootProjectDir
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

@GradlePluginTest(TestProjects.SINGLE_MODULE)
class KoverReportsTest {

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

    @ParameterizedTest
    @CsvSource(
        value = [
            "html,  html,       true",
//            "csv,   report.csv, false", // TODO
//            "xml,   report.xml, false"
        ]
    )
    fun `delta-coverage should create single report type`(
        reportToGenerate: String,
        expectedReportFile: String,
        isDirectory: Boolean
    ) {
        // setup
        val baseReportDir = "build/custom/reports/dir/jacoco/"
        buildFile.file.appendText(
            """
            deltaCoverageReport {
                coverageEngine = io.github.surpsg.deltacoverage.CoverageEngine.INTELLIJ
                diffSource.file.set('$diffFilePath')
                reportConfiguration.baseReportDir.set('$baseReportDir')
                reportConfiguration.$reportToGenerate.set(true)
            }
        """.trimIndent()
        )

        // disable jacoco auto-apply
        rootProjectDir.resolve("gradle.properties").appendText(
            """
            io.github.surpsg.delta-coverage.auto-apply-jacoco=false
        """.trimIndent()
        )

        // run // assert
        gradleRunner
            .runDeltaCoverageTask()
            .assertOutputContainsStrings("Fail on violations: false. Found violations: 0")

        // and assert
        val actualReport: File = rootProjectDir.resolve(baseReportDir)
            .resolve("deltaCoverage")
            .resolve(expectedReportFile)
        assertThat(actualReport).exists()
        assertThat(actualReport.isDirectory)
            .`as`("isDirectory")
            .isEqualTo(isDirectory)
    }

    @Test
    fun `delta-coverage should create deltaCoverage dir and full coverage with html, csv and xml reports`() {
        // setup
        val baseReportDir = "build/custom/reports/dir/jacoco/"
        buildFile.file.appendText(
            """
            
            deltaCoverageReport {
                diffSource {
                    file.set('$diffFilePath')
                }
                coverageBinaryFiles = jacocoTestReport.executionData
                classesDirs = jacocoTestReport.classDirectories
                srcDirs = jacocoTestReport.sourceDirectories
                
                reports {
                    html.set(true)
                    xml.set(true)
                    csv.set(true)
                    fullCoverageReport.set(true)
                    baseReportDir.set('$baseReportDir')
                }
            }
        """.trimIndent()
        )

        // run // assert
        gradleRunner.runDeltaCoverageTask()

        // and assert
        rootProjectDir.resolve(baseReportDir).apply {
            assertAllReportsCreated(resolve("deltaCoverage"))
            assertAllReportsCreated(resolve("fullReport"))
        }
    }

    private fun assertAllReportsCreated(baseReportDir: File) {
        assertThat(baseReportDir.list()).containsExactlyInAnyOrder("report.xml", "report.csv", "html")
        assertThat(baseReportDir.resolve("html").list())
            .containsExactlyInAnyOrder(
                *expectedHtmlReportFiles("com.java.test")
            )
    }
}

package io.github.surpsg.deltacoverage.gradle.kover

import io.github.surpsg.deltacoverage.gradle.TestProjects
import io.github.surpsg.deltacoverage.gradle.assertOutputContainsStrings
import io.github.surpsg.deltacoverage.gradle.runDeltaCoverageTask
import io.github.surpsg.deltacoverage.gradle.test.GradlePluginTest
import io.github.surpsg.deltacoverage.gradle.test.GradleRunnerInstance
import io.github.surpsg.deltacoverage.gradle.test.ProjectFile
import io.github.surpsg.deltacoverage.gradle.test.RestorableFile
import io.github.surpsg.deltacoverage.gradle.test.RootProjectDir
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.file.shouldBeADirectory
import io.kotest.matchers.file.shouldContainFile
import io.kotest.matchers.file.shouldExist
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@GradlePluginTest(resourceProjectDir = TestProjects.MULTI_MODULE, kts = true)
class KoverMultiModuleTest {

    @RootProjectDir
    lateinit var rootProjectDir: File

    @ProjectFile("test.diff")
    lateinit var diffFilePath: String

    @ProjectFile("build.gradle.kts")
    lateinit var buildFile: RestorableFile

    @GradleRunnerInstance
    lateinit var gradleRunner: GradleRunner

    @BeforeEach
    fun beforeEach() {
        buildFile.restoreOriginContent()
    }

    @Test
    fun `should build delta coverage report from multi module project `() {
        // GIVEN
        val baseReportDir = "build/custom/kover/"
        buildFile.file.appendText(
            """
            configure<DeltaCoverageConfiguration> {
                coverageEngine = CoverageEngine.INTELLIJ
                diffSource.file.set("$diffFilePath")
                reports {
                    html.set(true)
                    baseReportDir.set("$baseReportDir")
                }
                violationRules {
                    failIfCoverageLessThan(0.9)
                    failOnViolation.set(false)
                }
            }
        """.trimIndent()
        )

        // disable jacoco auto-apply
        rootProjectDir.resolve("gradle.properties").appendText(
            """
            io.github.surpsg.delta-coverage.auto-apply-jacoco=false // TODO
        """.trimIndent()
        )

        // WHEN // THEN
        gradleRunner
            .runDeltaCoverageTask()
            .assertOutputContainsStrings(
                "Fail on violations: false. Found violations: 3",
                "BRANCH: expectedMin=0.9, actual=0.5",
                "LINE: expectedMin=0.9, actual=1.0",
                "INSTRUCTION: expectedMin=0.9, actual=0.8",
            )

        // AND THEN
        val htmlReportDir = rootProjectDir.resolve(baseReportDir)
            .resolve(File("coverage-reports/delta-coverage/html"))
        assertSoftly(htmlReportDir) {
            shouldExist()
            shouldBeADirectory()
            shouldContainFile("index.html")
        }
    }
}
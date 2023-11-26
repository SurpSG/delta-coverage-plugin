package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.gradle.test.GradlePluginTest
import io.github.surpsg.deltacoverage.gradle.test.GradleRunnerInstance
import io.github.surpsg.deltacoverage.gradle.test.ProjectFile
import io.github.surpsg.deltacoverage.gradle.test.RestorableFile
import io.github.surpsg.deltacoverage.gradle.test.RootProjectDir
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

@Disabled
@GradlePluginTest(TestProjects.MULTI_MODULE)
class DeltaCoverageMultiModuleTest {

    @RootProjectDir
    lateinit var rootProjectDir: File

    @ProjectFile("test.diff")
    lateinit var diffFilePath: String

    @ProjectFile("build.gradle")
    lateinit var buildFile: RestorableFile

    @GradleRunnerInstance
    lateinit var gradleRunner: GradleRunner

    @BeforeEach
    fun beforeEach() {
        buildFile.restoreOriginContent()
    }

    @Test
    fun `delta-coverage should automatically collect jacoco configuration from submodules in multimodule project`() {
        // setup
        val baseReportDir = "build/custom/"
        val htmlReportDir = rootProjectDir.resolve(baseReportDir).resolve(File("deltaCoverage", "html"))
        buildFile.file.appendText(
            """
            
            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
                reports {
                    html.set(true)
                    baseReportDir.set('$baseReportDir')
                }
                violationRules.failIfCoverageLessThan 0.9
            }
        """.trimIndent()
        )

        // run // assert
        gradleRunner
            .runDeltaCoverageTaskAndFail()
            .assertOutputContainsStrings(
                "Fail on violations: true. Found violations: 1.",
                "Rule violated for bundle ${TestProjects.MULTI_MODULE}: " +
                        "branches covered ratio is 0.5, but expected minimum is 0.9"
            )

        // and assert
        assertThat(htmlReportDir.list()).containsExactlyInAnyOrder(
            *expectedHtmlReportFiles("com.module1", "com.module2")
        )
    }

    @Test
    fun `delta-coverage plugin should auto-apply jacoco to project and subprojects`() {
        // setup
        val expectedCoverageRatio = 0.8
        buildFile.file.writeText(rootBuildScriptWithoutJacocoPlugin(expectedCoverageRatio))

        // run // assert
        gradleRunner
            .runDeltaCoverageTaskAndFail()
            .assertOutputContainsStrings(
                "Fail on violations: true. Found violations: 1.",
                "Rule violated for bundle ${TestProjects.MULTI_MODULE}: " +
                        "branches covered ratio is 0.5, but expected minimum is $expectedCoverageRatio"
            )
    }

    @Test
    fun `delta-coverage plugin should not apply jacoco plugin if jacoco auto-apply is disabled`() {
        // setup
        buildFile.file.writeText(rootBuildScriptWithoutJacocoPlugin(1.0))

        // disable jacoco auto-apply
        rootProjectDir.resolve("gradle.properties").appendText(
            """
            io.github.surpsg.delta-coverage.auto-apply-jacoco=false
        """.trimIndent()
        )

        // manually apply jacoco only to 'module1'
        rootProjectDir.resolve("module1").resolve("build.gradle").appendText(
            """

            apply plugin: 'jacoco'
        """.trimIndent()
        )

        // run // assert
        gradleRunner.runDeltaCoverageTask()
    }

    private fun rootBuildScriptWithoutJacocoPlugin(expectedCoverageRatio: Double) = """
        plugins {
            id 'java'
            id 'io.github.surpsg.delta-coverage'
        }
        repositories {
            mavenCentral()
        }
        subprojects {
            apply plugin: 'java'
            repositories {
                mavenCentral()
            }
            dependencies {
                testImplementation 'junit:junit:4.13.2'
            }
        }
        deltaCoverageReport {
            diffSource.file.set('$diffFilePath')
            violationRules.failIfCoverageLessThan $expectedCoverageRatio
        }
    """.trimIndent()

}

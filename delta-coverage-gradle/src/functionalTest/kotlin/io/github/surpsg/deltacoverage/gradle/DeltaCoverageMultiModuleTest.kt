package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.gradle.test.GradlePluginTest
import io.github.surpsg.deltacoverage.gradle.test.GradleRunnerInstance
import io.github.surpsg.deltacoverage.gradle.test.ProjectFile
import io.github.surpsg.deltacoverage.gradle.test.RestorableFile
import io.github.surpsg.deltacoverage.gradle.test.RootProjectDir
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

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
        // GIVEN
        val baseReportDir = "build/custom/"
        buildFile.file.appendText(
            """
            
            deltaCoverageReport {
                coverage.engine.set(CoverageEngine.JACOCO)
            
                diffSource.file.set('$diffFilePath')
                reports {
                    html.set(true)
                    baseReportDir.set('$baseReportDir')
                }
                defaultReportView {
                    violationRules.failIfCoverageLessThan 0.9
                }
            }
        """.trimIndent()
        )

        // WHEN // THEN
        gradleRunner
            .runDeltaCoverageTaskAndFail()
            .assertOutputContainsStrings(
                "Fail on violations: true. Found violations: 1.",
                "Rule violated for bundle default: branches covered ratio is 0.5, but expected minimum is 0.9"
            )

        // and assert
        val htmlReportDir = rootProjectDir.resolve(baseReportDir).resolve("coverage-reports/delta-coverage/html")
        assertThat(htmlReportDir.list()).containsExactlyInAnyOrder(
            *expectedHtmlReportFiles("com.module1", "com.module2")
        )
    }

    @Test
    fun `deltaCoverage task should pass if coverage engine auto-apply disabled and jacoco applied manually`() {
        // GIVEN
        buildFile.file.writeText(
            """
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
                    tasks.withType(Test) {
                        useJUnitPlatform()
                    }
                
                    dependencies {
                        testImplementation(platform("org.junit:junit-bom:5.10.0"))
                        testImplementation("org.junit.jupiter:junit-jupiter")
                    }
                }
                deltaCoverageReport {
                    coverage {
                        engine.set(io.github.surpsg.deltacoverage.CoverageEngine.JACOCO)
                        autoApplyPlugin.set(false)
                    }
                    diffSource.file.set('$diffFilePath')
                    defaultReportView {
                        violationRules.failIfCoverageLessThan 0.7
                    }
                }
            """.trimIndent()
        )

        // manually apply jacoco only to 'module1'
        rootProjectDir.resolve("module1").resolve("build.gradle").appendText(
            """

            apply plugin: 'jacoco'
        """.trimIndent()
        )

        // WHEN // THEN
        gradleRunner
            .runDeltaCoverageTaskAndFail()
            .assertOutputContainsStrings(
                "Fail on violations: true. Found violations: 3.",
                "lines covered ratio is 0.5, but expected minimum is 0.7;",
                "branches covered ratio is 0.2, but expected minimum is 0.7;",
                "instructions covered ratio is 0.6, but expected minimum is 0.7",
            )
    }
}

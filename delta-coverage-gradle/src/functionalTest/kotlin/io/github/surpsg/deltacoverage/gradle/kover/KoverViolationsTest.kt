package io.github.surpsg.deltacoverage.gradle.kover

import io.github.surpsg.deltacoverage.gradle.TestProjects
import io.github.surpsg.deltacoverage.gradle.assertOutputContainsStrings
import io.github.surpsg.deltacoverage.gradle.runDeltaCoverageTask
import io.github.surpsg.deltacoverage.gradle.runDeltaCoverageTaskAndFail
import io.github.surpsg.deltacoverage.gradle.test.GradlePluginTest
import io.github.surpsg.deltacoverage.gradle.test.GradleRunnerInstance
import io.github.surpsg.deltacoverage.gradle.test.ProjectFile
import io.github.surpsg.deltacoverage.gradle.test.RestorableFile
import io.github.surpsg.deltacoverage.gradle.test.RootProjectDir
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@GradlePluginTest(TestProjects.SINGLE_MODULE, kts = true)
class KoverViolationsTest {

    @RootProjectDir
    lateinit var rootProjectDir: File

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

    @Test
    fun `delta-coverage should fail build if coverage rules are violated`() {
        // GIVEN
        buildFile.file.appendText(
            """
            configure<DeltaCoverageConfiguration> {
                coverage.engine = CoverageEngine.INTELLIJ
                diffSource.file = "$diffFilePath"
                defaultReportView {
                    violationRules failIfCoverageLessThan 1.0
                }
            }
        """.trimIndent()
        )

        // WHEN // THEN
        gradleRunner
            .runDeltaCoverageTaskAndFail()
            .assertOutputContainsStrings(
                "BRANCH: expectedMin=1.0, actual=0.75",
                "LINE: expectedMin=1.0, actual=0.6",
                "INSTRUCTION: expectedMin=1.0, actual=0.6"
            )
    }

    @Test
    fun `delta-coverage should not fail build if no branches`() {
        // GIVEN
        val diffFile: File = rootProjectDir.resolve("no-branches.diff")
        diffFile.writeText(
            """
                --- a/src/main/java/com/java/test/Class1.java
                +++ b/src/main/java/com/java/test/Class1.java
                @@ -16,10 +16,10 @@
                         } else {
                             result = 0;
                         }
                -        return result;
                +        return result; //
                     }
            """.trimIndent()
        )

        buildFile.file.appendText(
            """
            configure<DeltaCoverageConfiguration> {
                coverage.engine = CoverageEngine.INTELLIJ
                diffSource.file = "${diffFile.absolutePath}"
                defaultReportView {
                    violationRules {
                        failOnViolation = true
                        rule(io.github.surpsg.deltacoverage.gradle.CoverageEntity.BRANCH) {
                            minCoverageRatio = 1.0
                        }
                    }
                }
            }
        """.trimIndent()
        )

        // WHEN // THEN
        gradleRunner.runDeltaCoverageTask()
    }
}

package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.gradle.test.GradlePluginTest
import io.github.surpsg.deltacoverage.gradle.test.GradleRunnerInstance
import io.github.surpsg.deltacoverage.gradle.test.ProjectFile
import io.github.surpsg.deltacoverage.gradle.test.RestorableFile
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@GradlePluginTest(TestProjects.SINGLE_MODULE)
class DeltaCoverageGradleReleasesTest {

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
    @ValueSource(
        strings = [
            "5.6",
            "6.7.1",
            "7.6.4",
            "8.8", // the latest release or release candidate
        ]
    )
    fun `deltaCoverage task should be completed successfully on Gradle release`(
        gradleVersion: String
    ) {
        // setup
        buildFile.file.appendText(
            """
            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
            }
        """.trimIndent()
        )

        // run // assert
        gradleRunner
            .withGradleVersion(gradleVersion)
            .runDeltaCoverageTask()
            .assertDeltaCoverageStatusEqualsTo(SUCCESS)
    }
}

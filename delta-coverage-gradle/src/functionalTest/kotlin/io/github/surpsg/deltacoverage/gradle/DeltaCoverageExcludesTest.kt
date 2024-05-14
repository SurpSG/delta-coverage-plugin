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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name
import kotlin.streams.toList

@GradlePluginTest(TestProjects.EXCLUDE_CLASSES)
class DeltaCoverageExcludesTest {

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

    @Test
    fun `delta-coverage should not fail when all not covered classes are excluded`() {
        // setup
        val dollarSign = '$'
        buildFile.file.appendText(
            """

            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
               
               defaultReportView {
                    violationRules.failIfCoverageLessThan 1.0
               }
                
                excludeClasses.value([
                    '**/CoveredClass${dollarSign}UncoveredNestedClass.*',
                    '**/excludes/**/UncoveredClass.*', 
                    '**/excludes/sub/**/*.*'
                ])
                
                reports {
                    html.set(true)
                }
            }
        """.trimIndent()
        )

        // run // assert
        gradleRunner
            .runDeltaCoverageTask()
            .assertOutputContainsStrings("Fail on violations: true. Found violations: 0")

        // and assert
        val htmlReportDir: Path = rootProjectDir.toPath()
            .resolve("build/reports/coverage-reports/delta-coverage/html/")
        val classReportFiles: List<Path> = findAllFiles(htmlReportDir) { file ->
            file.name.endsWith("Class.html")
        }
        assertThat(classReportFiles)
            .hasSize(1).first()
            .extracting(Path::name)
            .isEqualTo("CoveredClass.html")
    }

    @Test
    fun `delta-coverage should fail when not covered classes are not excluded`() {
        // setup
        buildFile.file.appendText(
            """

            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
               
                defaultReportView {
                    violationRules.failIfCoverageLessThan 1.0
               }
                
                excludeClasses.value([])
            }
        """.trimIndent()
        )

        // run // assert
        gradleRunner
            .runDeltaCoverageTaskAndFail()
            .assertOutputContainsStrings("Fail on violations: true. Found violations: 2")
    }

    private fun findAllFiles(rootDir: Path, fileFilter: (Path) -> Boolean): List<Path> {
        return Files.find(
            rootDir,
            Int.MAX_VALUE,
            { filePath: Path, _: BasicFileAttributes -> fileFilter(filePath) }
        ).toList()
    }

}

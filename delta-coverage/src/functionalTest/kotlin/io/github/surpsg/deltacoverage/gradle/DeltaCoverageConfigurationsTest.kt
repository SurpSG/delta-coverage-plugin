package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.diff.git.getCrlf
import io.github.surpsg.deltacoverage.gradle.DeltaCoveragePlugin.Companion.DELTA_COVERAGE_TASK
import io.github.surpsg.deltacoverage.gradle.resources.getResourceFile
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class DeltaCoverageConfigurationsTest : BaseDeltaCoverageTest() {

    companion object {
        const val TEST_PROJECT_RESOURCE_NAME = "single-module-test-project"

        private const val MOCK_SERVER_PORT = 8888
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

    @Test
    fun `delta-coverage should fail if classes file collection is empty`() {
        // setup
        buildFile.appendText(
            """
            deltaCoverageReport {
                diffSource.file.set('$diffFilePath')
                classesDirs = files()
            }
        """.trimIndent()
        )

        // run
        val result = gradleRunner.runTaskAndFail(DELTA_COVERAGE_TASK)

        // assert
        result.assertOutputContainsStrings("'deltaCoverageReport.classesDirs' file collection is empty.")
    }

    @Test
    fun `delta-coverage should use git to generate diff`() {
        // setup
        prepareTestProjectWithGit()

        buildFile.delete()
        buildFile = File(rootProjectDir, "build.gradle.kts")
        buildFile.writeText(
            """
            import io.github.surpsg.deltacoverage.gradle.DeltaCoverageConfiguration
                
            plugins {
                java
                id("io.github.surpsg.delta-coverage")
            }
            
            repositories {
                mavenCentral()
            }

            dependencies {
                testImplementation("junit:junit:4.13.2")
            }

            configure<DeltaCoverageConfiguration> {
                diffSource.git compareWith "HEAD"
                violationRules failIfCoverageLessThan 0.7
            }
        """.trimIndent()
        )

        // run
        val result = gradleRunner.runTaskAndFail(DELTA_COVERAGE_TASK)

        // assert
        result.assertDeltaCoverageStatusEqualsTo(FAILED)
            .assertOutputContainsStrings(
                "instructions covered ratio is 0.5, but expected minimum is 0.7",
                "branches covered ratio is 0.5, but expected minimum is 0.7",
                "lines covered ratio is 0.6, but expected minimum is 0.7"
            )
    }

    @Test
    fun `delta-coverage should get diff info by url`() {
        // setup
        buildFile.appendText(
            """

            deltaCoverageReport {
                diffSource.url.set('http://localhost:$MOCK_SERVER_PORT/')
                violationRules {
                    minInstructions.set(1d)
                    failOnViolation.set(true)
                }
            }
        """.trimIndent()
        )

        MockHttpServer(MOCK_SERVER_PORT, File(diffFilePath).readText()).use {
            // run
            val result = gradleRunner.runTaskAndFail(DELTA_COVERAGE_TASK)

            // assert
            result.assertDeltaCoverageStatusEqualsTo(FAILED)
                .assertOutputContainsStrings("instructions covered ratio is 0.5, but expected minimum is 1")
        }
    }

    @Test
    fun `delta-coverage should fail and print available branches if provided branch not found`() {
        // setup
        val unknownBranch = "unknown-branch"
        val newBranch = "new-branch"
        buildGitRepository().use { git ->
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Add all").call()
            git.branchCreate().setName(newBranch).call()
        }

        buildFile.appendText(
            """

            deltaCoverageReport {
                diffSource.git.compareWith '$unknownBranch'
            }
        """.trimIndent()
        )

        // run
        val result = gradleRunner.runTaskAndFail(DELTA_COVERAGE_TASK)

        // assert
        result.assertDeltaCoverageStatusEqualsTo(FAILED)
            .assertOutputContainsStrings(
                "Unknown revision '$unknownBranch'",
                "Available branches: refs/heads/master, refs/heads/$newBranch"
            )
    }

    private fun prepareTestProjectWithGit() {
        rootProjectDir.resolve(".gitignore").apply {
            appendText("\n*")
            appendText("\n!*.java")
            appendText("\n!gitignore")
            appendText("\n!*/")
        }
        buildGitRepository().use { git ->
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Add all").call()

            val oldVersionFile = "src/main/java/com/java/test/Class1.java"
            val targetFile = rootProjectDir.resolve(oldVersionFile)
            getResourceFile<DeltaCoverageConfigurationsTest>("git-diff-source-test-files/Class1GitTest.java")
                .copyTo(targetFile, true)

            git.add().addFilepattern(oldVersionFile).call()
            git.commit().setMessage("Added old file version").call()

            getResourceFile<DeltaCoverageConfigurationsTest>("$TEST_PROJECT_RESOURCE_NAME/src").copyRecursively(
                rootProjectDir.resolve("src"),
                true
            )
            git.add().addFilepattern(".").call()
        }
    }

    private fun buildGitRepository(): Git {
        val repository: Repository = FileRepositoryBuilder.create(File(rootProjectDir, ".git")).apply {
            config.setEnum(
                ConfigConstants.CONFIG_CORE_SECTION,
                null,
                ConfigConstants.CONFIG_KEY_AUTOCRLF,
                getCrlf()
            )
            create()
        }
        return Git(repository)
    }

}

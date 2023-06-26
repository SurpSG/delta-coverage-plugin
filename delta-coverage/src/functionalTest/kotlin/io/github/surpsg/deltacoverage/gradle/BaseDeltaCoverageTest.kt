package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.gradle.resources.copyDirFromResources
import io.github.surpsg.deltacoverage.gradle.resources.toUnixAbsolutePath
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class BaseDeltaCoverageTest {

    @TempDir
    lateinit var tempTestDir: File

    lateinit var rootProjectDir: File
    lateinit var buildFile: File
    lateinit var diffFilePath: String
    lateinit var gradleRunner: GradleRunner

    /**
     * should be invoked in @Before test class method
     */
    fun initializeGradleTest() {
        val configuration: TestConfiguration = buildTestConfiguration()

        rootProjectDir = tempTestDir.copyDirFromResources<BaseDeltaCoverageTest>(configuration.resourceTestProject)
        diffFilePath = rootProjectDir.resolve(configuration.diffFilePath).toUnixAbsolutePath()
        buildFile = rootProjectDir.resolve(configuration.rootBuildFilePath)

        gradleRunner = buildGradleRunner(rootProjectDir).apply {
            runTask("test")
        }
    }

    abstract fun buildTestConfiguration(): TestConfiguration
}

class TestConfiguration(
    val resourceTestProject: String,
    val rootBuildFilePath: String,
    val diffFilePath: String,
)

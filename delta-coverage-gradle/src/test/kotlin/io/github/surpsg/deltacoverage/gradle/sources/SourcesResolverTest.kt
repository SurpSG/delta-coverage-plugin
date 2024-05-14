package io.github.surpsg.deltacoverage.gradle.sources

import io.github.surpsg.deltacoverage.CoverageEngine
import io.github.surpsg.deltacoverage.gradle.sources.lookup.JacocoPluginSourcesLookup.Companion.JACOCO_REPORT_TASK
import io.github.surpsg.deltacoverage.gradle.unittest.testJavaProject
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.testing.jacoco.tasks.JacocoReportBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import io.github.surpsg.deltacoverage.gradle.DeltaCoverageConfiguration as DeltaConfig

internal class SourcesResolverTest {

    private val sourcesResolver = SourcesResolver()

    @ParameterizedTest
    @MethodSource("delta coverage source test parameters")
    fun `should return delta coverage configured files`(
        sourceType: SourceType,
        deltaConfigSetter: (DeltaConfig, FileCollection) -> Unit,
    ) {
        // GIVEN
        val expectedFile = "expected/path/$sourceType"
        val project = testJavaProject()
        val context: SourcesResolver.Context = testJavaProject().sourceContext(sourceType) {
            coverage.engine.set(CoverageEngine.JACOCO)
            deltaConfigSetter(this, project.files(expectedFile))
        }

        // WHEN
        val resolvedFiles: FileCollection = sourcesResolver.resolve(context)

        // THEN
        assertSoftly(resolvedFiles) {
            shouldHaveSize(1)
            first().path shouldEndWith expectedFile
        }
    }

    @Test
    fun `should return delta coverage source code files`() {
        // GIVEN
        val context: SourcesResolver.Context = testJavaProject().sourceContext(SourceType.SOURCES) {
            coverage.engine.set(CoverageEngine.JACOCO)
        }

        // WHEN
        val resolvedFiles: FileCollection = sourcesResolver.resolve(context)

        // THEN
        assertSoftly(resolvedFiles) {
            shouldHaveSize(1)
            first().path shouldEndWith "src/main/java"
        }
    }

    @ParameterizedTest
    @MethodSource("delta coverage source test parameters")
    fun `should throw if source is empty and delta-cov sources are empty`(
        sourceType: SourceType,
        deltaConfigSetter: (DeltaConfig, FileCollection) -> Unit,
    ) {
        // GIVEN
        val project = testJavaProject()
        val emptyFiles = project.files()
        val context: SourcesResolver.Context = project.sourceContext(sourceType) {
            coverage.engine.set(CoverageEngine.JACOCO)
            deltaConfigSetter(this, emptyFiles)
        }

        // WHEN // THEN
        val actualException = shouldThrow<IllegalStateException> {
            sourcesResolver.resolve(context)
        }

        // AND THEN
        assertSoftly(actualException.message) {
            shouldContain(sourceType.sourceConfigurationPath)
            shouldContain("file collection is empty")
        }
    }

    @Test
    fun `should throw if coverage binaries are empty and delta-cov sources are not set and jacoco is not applied`() {
        // GIVEN
        val project = testJavaProject()
        val context: SourcesResolver.Context = project.sourceContext(SourceType.COVERAGE_BINARIES) {
            coverage.engine.set(CoverageEngine.JACOCO)
            reportViews.register(VIEW_NAME)
        }

        // WHEN // THEN
        val actualException = shouldThrow<IllegalStateException> {
            sourcesResolver.resolve(context)
        }

        // AND THEN
        assertSoftly(actualException.message) {
            shouldContain(SourceType.COVERAGE_BINARIES.sourceConfigurationPath)
            shouldContain("is not configured")
        }
    }

    @Test
    fun `should throw if coverage binaries are empty and delta-cov sources are not set and jacoco files is empty`() {
        // GIVEN
        val project = testJavaProject {
            pluginManager.apply("jacoco")

            val jacocoReport: JacocoReportBase = tasks.findByPath(JACOCO_REPORT_TASK) as JacocoReportBase
            jacocoReport.executionData.setFrom(files())
        }

        val context: SourcesResolver.Context = project.sourceContext(SourceType.COVERAGE_BINARIES) {
            coverage.engine.set(CoverageEngine.JACOCO)
            reportViews.register(VIEW_NAME)
        }

        // WHEN // THEN
        val actualException = shouldThrow<IllegalStateException> {
            sourcesResolver.resolve(context)
        }

        // AND THEN
        assertSoftly(actualException.message) {
            shouldContain(SourceType.COVERAGE_BINARIES.sourceConfigurationPath)
            shouldContain("is not configured.")
        }
    }

    private fun Project.sourceContext(
        sourceType: SourceType,
        customize: DeltaConfig.() -> Unit
    ): SourcesResolver.Context {
        return SourcesResolver.Context.Builder
            .newBuilder(
                project,
                project.objects,
                project.objects.newInstance(DeltaConfig::class.java).apply(customize)
            )
            .build(VIEW_NAME, sourceType)
    }

    companion object {
        const val VIEW_NAME = "viewName"

        @JvmStatic
        fun `delta coverage source test parameters`(): List<Arguments> {
            return listOf<Arguments>(
                arguments(
                    SourceType.CLASSES,
                    { config: DeltaConfig, files: FileCollection -> config.classesDirs = files },
                ),

                arguments(
                    SourceType.COVERAGE_BINARIES,
                    { config: DeltaConfig, files: FileCollection ->
                        config.reportViews.register(VIEW_NAME) { view ->
                            view.coverageBinaryFiles = files
                        }
                    },
                ),
            )
        }
    }
}

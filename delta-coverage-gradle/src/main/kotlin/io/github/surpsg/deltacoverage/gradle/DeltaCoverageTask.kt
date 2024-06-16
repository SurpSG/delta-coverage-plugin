package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.diff.DiffSource
import io.github.surpsg.deltacoverage.gradle.config.ConfigMapper
import io.github.surpsg.deltacoverage.report.DeltaReportFacadeFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject
import io.github.surpsg.deltacoverage.gradle.DeltaCoverageConfiguration as GradleDeltaCoverageConfig

open class DeltaCoverageTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    init {
        group = "verification"
        description = "Builds coverage report only for modified code"
        outputs.upToDateWhen { false }
    }

    @get:Nested
    val coverageBinaryFiles: MapProperty<String, FileCollection> = objectFactory.mapProperty(
        String::class.java,
        FileCollection::class.java
    )

    @get:InputFiles
    val sourcesFiles: Property<FileCollection> = objectFactory.property(FileCollection::class.java)

    @get:InputFiles
    val classesFiles: Property<FileCollection> = objectFactory.property(FileCollection::class.java)

    @Nested
    val deltaCoverageConfigProperty: Property<GradleDeltaCoverageConfig> = objectFactory.property(
        GradleDeltaCoverageConfig::class.java
    )

    private val projectDirProperty: File
        get() = project.projectDir

    private val rootProjectDirProperty: File
        get() = project.rootProject.projectDir

    @OutputDirectory
    fun getOutputDir(): File {
        val baseReportDirPath: String = deltaCoverageConfigProperty.get().reportConfiguration.baseReportDir.get()
        val file = File(baseReportDirPath)
        return if (file.isAbsolute) {
            file
        } else {
            projectDirProperty.resolve(baseReportDirPath)
        }.resolve(BASE_COVERAGE_REPORTS_DIR)
    }

    @TaskAction
    fun executeAction() {
        val gradleCoverageConfig: GradleDeltaCoverageConfig = deltaCoverageConfigProperty.get()
        log.info("Delta-Coverage plugin configuration: $gradleCoverageConfig")

        val diffSource: DiffSource = obtainDiffSource(getOutputDir(), gradleCoverageConfig)
        val deltaCoverageConfigs: List<DeltaCoverageConfig> = buildDeltaCoverageConfigs(
            diffSource,
            gradleCoverageConfig,
        )
        runDeltaCoverage(deltaCoverageConfigs)
    }

    private fun obtainDiffSource(
        reportDir: File,
        gradleCoverageConfig: GradleDeltaCoverageConfig,
    ): DiffSource = ConfigMapper.convertToDiffSource(
        rootProjectDirProperty,
        gradleCoverageConfig.diffSource,
    ).apply {
        val savedFile = saveDiffTo(reportDir)
        log.info("Diff content saved to file://{}", savedFile.absolutePath)
    }

    private fun runDeltaCoverage(
        deltaCoverageConfigs: List<DeltaCoverageConfig>
    ) {
        deltaCoverageConfigs.forEach { config ->
            if (log.isDebugEnabled) {
                log.debug("Run Delta-Coverage with config: {}", config)
            }

            DeltaReportFacadeFactory
                .buildFacade(config)
                .generateReport()
        }
    }

    private fun buildDeltaCoverageConfigs(
        diffSource: DiffSource,
        gradleCoverageConfig: GradleDeltaCoverageConfig,
    ): List<DeltaCoverageConfig> {
        return gradleCoverageConfig.reportViews.asSequence()
            .map { it.name }
            .map { viewName ->
                ConfigMapper.convertToCoreConfig(
                    viewName = viewName,
                    reportLocation = getOutputDir(),
                    diffSource = diffSource,
                    deltaCoverageConfig = gradleCoverageConfig,
                    sourcesFiles = sourcesFiles.get().files,
                    classesFiles = classesFiles.get().files,
                    coverageBinaryFiles = coverageBinaryFiles.getting(viewName).get().files,
                )
            }
            .toList()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(DeltaCoverageTask::class.java)
        const val BASE_COVERAGE_REPORTS_DIR = "coverage-reports"
    }
}

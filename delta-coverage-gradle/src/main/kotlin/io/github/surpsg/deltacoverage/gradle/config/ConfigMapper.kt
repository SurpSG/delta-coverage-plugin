package io.github.surpsg.deltacoverage.gradle.config

import io.github.surpsg.deltacoverage.config.CoverageEntity
import io.github.surpsg.deltacoverage.config.CoverageRulesConfig
import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.config.DiffSourceConfig
import io.github.surpsg.deltacoverage.config.ReportConfig
import io.github.surpsg.deltacoverage.config.ReportsConfig
import io.github.surpsg.deltacoverage.config.ViolationRule
import io.github.surpsg.deltacoverage.diff.DiffSource
import io.github.surpsg.deltacoverage.gradle.DiffSourceConfiguration
import io.github.surpsg.deltacoverage.gradle.ViolationRules
import java.io.File
import io.github.surpsg.deltacoverage.gradle.CoverageEntity as GradleCoverageEntity
import io.github.surpsg.deltacoverage.gradle.DeltaCoverageConfiguration as GradleDeltaCoverageConfig
import io.github.surpsg.deltacoverage.gradle.ViolationRule as GradleViolationRule

internal object ConfigMapper {

    fun convertToDiffSource(
        projectRoot: File,
        diffSourceConfig: DiffSourceConfiguration
    ): DiffSource {
        val diffConfig = DiffSourceConfig {
            if (diffSourceConfig.git.useNativeGit.get()) {
                file = diffSourceConfig.git.nativeGitDiffFile.get().asFile.absolutePath
            } else {
                file = diffSourceConfig.file.get()
                url = diffSourceConfig.url.get()
                diffBase = diffSourceConfig.git.diffBase.get()
            }
        }
        return DiffSource.buildDiffSource(projectRoot, diffConfig)
    }

    @Suppress("LongParameterList")
    fun convertToCoreConfig(
        viewName: String,
        reportLocation: File,
        diffSource: DiffSource,
        deltaCoverageConfig: GradleDeltaCoverageConfig,
        sourcesFiles: Set<File>,
        classesFiles: Set<File>,
        coverageBinaryFiles: Set<File>,
    ) = DeltaCoverageConfig {
        coverageEngine = deltaCoverageConfig.coverage.engine.get()
        reportName = viewName
        this.diffSource = diffSource

        binaryCoverageFiles += coverageBinaryFiles
        sourceFiles += sourcesFiles
        classFiles += classesFiles

        reportsConfig = ReportsConfig {
            baseReportDir = reportLocation.absolutePath
            html = ReportConfig {
                outputFileName = "html"
                enabled = deltaCoverageConfig.reportConfiguration.html.get()
            }
            csv = ReportConfig {
                outputFileName = "report.csv"
                enabled = deltaCoverageConfig.reportConfiguration.csv.get()
            }
            xml = ReportConfig {
                outputFileName = "report.xml"
                enabled = deltaCoverageConfig.reportConfiguration.xml.get()
            }
            console = ReportConfig {
                outputFileName = "console.txt"
                enabled = deltaCoverageConfig.reportConfiguration.console.get()
            }
            markdown = ReportConfig {
                outputFileName = "report.md"
                enabled = deltaCoverageConfig.reportConfiguration.markdown.get()
            }
            fullCoverageReport = deltaCoverageConfig.reportConfiguration.fullCoverageReport.get()
        }

        coverageRulesConfig = buildCoverageRulesConfig(viewName, deltaCoverageConfig)
    }

    private fun buildCoverageRulesConfig(
        viewName: String,
        diffCovConfig: GradleDeltaCoverageConfig
    ) = CoverageRulesConfig {
        val rules: ViolationRules = diffCovConfig.reportViews.getByName(viewName).violationRules
        violationRules += rules.rules.get().map { (entity, rule) ->
            buildCoreViolationRule(entity, rule)
        }
        failOnViolation = rules.failOnViolation.get()
    }

    private fun buildCoreViolationRule(
        entity: GradleCoverageEntity,
        rule: GradleViolationRule,
    ): ViolationRule = ViolationRule {
        coverageEntity = entity.mapToCoreCoverageEntity()
        minCoverageRatio = rule.minCoverageRatio.get()
        rule.entityCountThreshold.orNull?.let { threshold ->
            entityCountThreshold = threshold
        }
    }

    private fun GradleCoverageEntity.mapToCoreCoverageEntity(): CoverageEntity = when (this) {
        GradleCoverageEntity.INSTRUCTION -> CoverageEntity.INSTRUCTION
        GradleCoverageEntity.BRANCH -> CoverageEntity.BRANCH
        GradleCoverageEntity.LINE -> CoverageEntity.LINE
    }
}

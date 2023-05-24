package io.github.surpsg.deltacoverage.report

import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.diff.DiffSource
import io.github.surpsg.deltacoverage.diff.diffSourceFactory
import io.github.surpsg.deltacoverage.report.intellij.IntellijDeltaReportGeneratorFacade
import io.github.surpsg.deltacoverage.report.jacoco.JacocoDeltaReportGeneratorFacade
import java.io.File

object DeltaReportFacadeFactory {

    fun buildFacade(
        projectRoot: File,
        coverageEngine: CoverageEngine,
        deltaCoverageConfig: DeltaCoverageConfig
    ): DeltaReportGeneratorFacade {
        val diffSource: DiffSource = diffSourceFactory(projectRoot, deltaCoverageConfig.diffSourceConfig)
        val reportContext = ReportContext(diffSource, deltaCoverageConfig)

        return when (coverageEngine) {
            CoverageEngine.JACOCO -> JacocoDeltaReportGeneratorFacade(reportContext)
            CoverageEngine.INTELLIJ -> IntellijDeltaReportGeneratorFacade(reportContext)
        }
    }

}

package io.github.surpsg.deltacoverage.report

import io.github.surpsg.deltacoverage.CoverageEngine
import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.report.intellij.IntellijDeltaReportGeneratorFacade
import io.github.surpsg.deltacoverage.report.jacoco.JacocoDeltaReportGeneratorFacade

object DeltaReportFacadeFactory {

    fun buildFacade(
        deltaCoverageConfig: DeltaCoverageConfig
    ): DeltaReportGeneratorFacade {
        return ReportContext(deltaCoverageConfig).let { context ->
            when (deltaCoverageConfig.coverageEngine) {
                CoverageEngine.JACOCO -> JacocoDeltaReportGeneratorFacade(context)
                CoverageEngine.INTELLIJ -> IntellijDeltaReportGeneratorFacade(context)
            }
        }
    }
}

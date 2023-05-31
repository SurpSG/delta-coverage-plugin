package io.github.surpsg.deltacoverage.report.intellij

import com.intellij.rt.coverage.report.ReportLoadStrategy
import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.report.DeltaReportGeneratorFacade
import io.github.surpsg.deltacoverage.report.ReportContext
import io.github.surpsg.deltacoverage.report.intellij.coverage.ReportLoadStrategyFactory
import io.github.surpsg.deltacoverage.report.intellij.report.CoverageReportFactory
import io.github.surpsg.deltacoverage.report.intellij.verifier.CoverageAssertion

internal class IntellijDeltaReportGeneratorFacade(
    reportContext: ReportContext
) : DeltaReportGeneratorFacade(reportContext) {

    override fun generateReport(): DeltaReportGeneratorFacade {
        generate(reportContext.deltaCoverageConfig)

        return this
    }

    private fun generate(deltaCoverageConfig: DeltaCoverageConfig) {
        val reportLoadStrategy: ReportLoadStrategy = ReportLoadStrategyFactory.buildReportLoadStrategy(reportContext)

        CoverageReportFactory
            .reportBuildersBy(deltaCoverageConfig, reportLoadStrategy)
            .forEach { reportBuilder ->
                reportBuilder.buildReport()
            }

        CoverageAssertion.verify(reportLoadStrategy.projectData, deltaCoverageConfig.violationRuleConfig)
    }

}

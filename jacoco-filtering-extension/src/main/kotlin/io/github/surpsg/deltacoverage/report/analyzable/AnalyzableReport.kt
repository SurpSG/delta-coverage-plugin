package io.github.surpsg.deltacoverage.report.analyzable

import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.diff.DiffSource
import io.github.surpsg.deltacoverage.report.DiffReport
import io.github.surpsg.deltacoverage.report.reportFactory
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.ICoverageVisitor
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.report.IReportVisitor

internal interface AnalyzableReport {

    fun buildVisitor(): IReportVisitor
    fun buildAnalyzer(executionDataStore: ExecutionDataStore, coverageVisitor: ICoverageVisitor): Analyzer
}

internal fun analyzableReportFactory(
    deltaCoverageConfig: DeltaCoverageConfig,
    diffSource: DiffSource
): Set<AnalyzableReport> {
    return reportFactory(deltaCoverageConfig, diffSource)
        .map { reportMode ->
            when (reportMode) {
                is DiffReport -> DeltaCoverageAnalyzableReport(deltaCoverageConfig.coverageRulesConfig, reportMode)
                else -> FullCoverageAnalyzableReport(reportMode)
            }
        }.toSet()
}



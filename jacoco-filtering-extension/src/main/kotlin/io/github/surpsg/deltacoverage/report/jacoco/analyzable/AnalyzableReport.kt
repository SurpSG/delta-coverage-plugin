package io.github.surpsg.deltacoverage.report.jacoco.analyzable

import io.github.surpsg.deltacoverage.report.JacocoDeltaReport
import io.github.surpsg.deltacoverage.report.ReportContext
import io.github.surpsg.deltacoverage.report.jacoco.reportFactory
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.ICoverageVisitor
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.report.IReportVisitor

internal interface AnalyzableReport {

    fun buildVisitor(): IReportVisitor
    fun buildAnalyzer(executionDataStore: ExecutionDataStore, coverageVisitor: ICoverageVisitor): Analyzer
}

internal fun analyzableReportFactory(
    reportContext: ReportContext
): Set<AnalyzableReport> {
    return reportFactory(reportContext)
        .map { reportMode ->
            when (reportMode) {
                is JacocoDeltaReport -> DeltaCoverageAnalyzableReport(reportMode)
                else -> FullCoverageAnalyzableReport(reportMode)
            }
        }.toSet()
}



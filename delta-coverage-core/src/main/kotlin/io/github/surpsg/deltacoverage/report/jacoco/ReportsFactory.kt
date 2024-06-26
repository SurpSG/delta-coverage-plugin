package io.github.surpsg.deltacoverage.report.jacoco

import io.github.surpsg.deltacoverage.config.ReportsConfig
import io.github.surpsg.deltacoverage.report.EnabledReportTypeFactory
import io.github.surpsg.deltacoverage.report.FullReport
import io.github.surpsg.deltacoverage.report.JacocoDeltaReport
import io.github.surpsg.deltacoverage.report.JacocoReport
import io.github.surpsg.deltacoverage.report.ReportContext
import io.github.surpsg.deltacoverage.report.ReportType
import io.github.surpsg.deltacoverage.report.Violations
import io.github.surpsg.deltacoverage.report.ReportBound

internal fun reportFactory(
    reportContext: ReportContext
): Set<FullReport> {
    val reportsConfig: ReportsConfig = reportContext.deltaCoverageConfig.reportsConfig
    val reportTypes: Set<ReportType> = EnabledReportTypeFactory.obtain(reportsConfig)

    val allReports: MutableSet<FullReport> = mutableSetOf()
    allReports += JacocoDeltaReport(
        ReportBound.DELTA_REPORT.buildJacocoReports(reportsConfig, reportTypes),
        reportContext.codeUpdateInfo,
        Violations(
            reportContext.deltaCoverageConfig.coverageRulesConfig.failOnViolation,
            JacocoVerifierFactory.buildRules(reportContext.deltaCoverageConfig.coverageRulesConfig)
        )
    )

    if (reportsConfig.fullCoverageReport) {
        allReports += FullReport(
            ReportBound.FULL_REPORT.buildJacocoReports(reportsConfig, reportTypes)
        )
    }
    return allReports
}

private fun ReportBound.buildJacocoReports(
    reportsConfig: ReportsConfig,
    reportTypes: Set<ReportType>
): List<JacocoReport> {
    return reportTypes.map { reportType ->
        JacocoReport(
            reportType,
            this,
            reportsConfig,
        )
    }
}

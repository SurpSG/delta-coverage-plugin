package io.github.surpsg.deltacoverage.report.jacoco

import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.config.ReportsConfig
import io.github.surpsg.deltacoverage.report.FullReport
import io.github.surpsg.deltacoverage.report.JacocoDeltaReport
import io.github.surpsg.deltacoverage.report.Report
import io.github.surpsg.deltacoverage.report.ReportContext
import io.github.surpsg.deltacoverage.report.ReportType
import io.github.surpsg.deltacoverage.report.Violation
import org.jacoco.report.check.Rule
import java.nio.file.Paths

internal fun reportFactory(
    reportContext: ReportContext
): Set<FullReport> {
    val diffSourceConfig: DeltaCoverageConfig = reportContext.deltaCoverageConfig

    val reports: Set<Report> = diffSourceConfig.reportsConfig.toReportTypes()

    val violationRule: Rule = buildRule(diffSourceConfig.violationRuleConfig)
    val baseReportDir = Paths.get(diffSourceConfig.reportsConfig.baseReportDir)
    val report: MutableSet<FullReport> = mutableSetOf(
        JacocoDeltaReport(
            baseReportDir.resolve("deltaCoverage"),
            reports,
            reportContext.codeUpdateInfo,
            Violation(
                diffSourceConfig.violationRuleConfig.failOnViolation,
                listOf(violationRule)
            )
        )
    )

    if (diffSourceConfig.reportsConfig.fullCoverageReport) {
        report += FullReport(
            baseReportDir.resolve("fullReport"),
            reports
        )
    }

    return report
}

private fun ReportsConfig.toReportTypes(): Set<Report> = sequenceOf(
    ReportType.HTML to html,
    ReportType.CSV to csv,
    ReportType.XML to xml
).filter { it.second.enabled }.map {
    Report(it.first, it.second.outputFileName)
}.toSet()

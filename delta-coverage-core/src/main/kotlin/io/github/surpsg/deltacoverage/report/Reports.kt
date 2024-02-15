package io.github.surpsg.deltacoverage.report

import io.github.surpsg.deltacoverage.config.ReportsConfig
import io.github.surpsg.deltacoverage.diff.CodeUpdateInfo
import io.github.surpsg.deltacoverage.report.intellij.report.ReportBound
import io.github.surpsg.deltacoverage.report.intellij.report.ReportPathStrategy
import org.jacoco.report.check.Rule
import java.io.File

open class FullReport(
    val jacocoReports: List<JacocoReport>
) {
    fun resolveReportAbsolutePath(jacocoReport: JacocoReport): File {
        val reportPathStrategy = when (jacocoReport.reportType) {
            ReportType.HTML -> ReportPathStrategy.Html(jacocoReport.reportsConfig)
            ReportType.XML -> ReportPathStrategy.Xml(jacocoReport.reportsConfig)
            ReportType.CSV -> ReportPathStrategy.Csv(jacocoReport.reportsConfig)
        }
        return reportPathStrategy.buildReportPath(jacocoReport.reportBound)
    }
}

class JacocoDeltaReport(
    reports: List<JacocoReport>,
    val codeUpdateInfo: CodeUpdateInfo,
    val violations: Violations
) : FullReport(reports)

data class JacocoReport(
    val reportType: ReportType,
    val reportBound: ReportBound,
    val reportsConfig: ReportsConfig
)

enum class ReportType {
    HTML,
    XML,
    CSV,
}

data class Violations(
    val failOnViolation: Boolean,
    val violationRules: List<Rule>
)

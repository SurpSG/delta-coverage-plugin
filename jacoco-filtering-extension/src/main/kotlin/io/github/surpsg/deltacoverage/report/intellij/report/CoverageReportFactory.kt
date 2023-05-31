package io.github.surpsg.deltacoverage.report.intellij.report

import com.intellij.rt.coverage.report.ReportLoadStrategy
import com.intellij.rt.coverage.report.Reporter
import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.config.ReportsConfig
import io.github.surpsg.deltacoverage.report.ReportType

internal object CoverageReportFactory {

    fun reportBuildersBy(
        deltaCoverageConfig: DeltaCoverageConfig,
        reportLoadStrategy: ReportLoadStrategy
    ): Iterable<ReportBuilder> {
        val reporter = Reporter(reportLoadStrategy)
        return sequenceOf(
            ReportType.HTML to deltaCoverageConfig.reportsConfig.html.enabled,
            ReportType.XML to deltaCoverageConfig.reportsConfig.xml.enabled,
            ReportType.CSV to deltaCoverageConfig.reportsConfig.csv.enabled,
        )
            .filter { (_, enabled) -> enabled }
            .map { (reportType, _) ->
                reportType.buildReportBuilder(
                    deltaCoverageConfig.reportName,
                    deltaCoverageConfig.reportsConfig,
                    reporter
                )
            }
            .toList()
    }

    private fun ReportType.buildReportBuilder(
        reportName: String,
        reportsConfig: ReportsConfig,
        reporter: Reporter,
    ): ReportBuilder {
        return when (this) {
            ReportType.HTML -> HtmlReportBuilder(reportName, reportsConfig, reporter)
            ReportType.XML -> XmlReportBuilder(reportsConfig, reporter)
            else -> error("Unsupported report type: $this")
        }
    }

}

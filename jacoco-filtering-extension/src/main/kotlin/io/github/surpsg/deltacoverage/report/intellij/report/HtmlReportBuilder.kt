package io.github.surpsg.deltacoverage.report.intellij.report

import com.intellij.rt.coverage.report.Reporter
import io.github.surpsg.deltacoverage.config.ReportsConfig
import java.io.File

internal class HtmlReportBuilder(
    private val reportName: String,
    reportsConfig: ReportsConfig,
    reporter: Reporter,
) : ReportBuilder(reporter, reportsConfig) {

    override val reportOutputFilePath: String = reportsConfig.html.outputFileName

    override fun buildReport(reportPath: File, reporter: Reporter) {
        reporter.createHTMLReport(
            reportPath,
            reportName,
            null
        )
    }

}

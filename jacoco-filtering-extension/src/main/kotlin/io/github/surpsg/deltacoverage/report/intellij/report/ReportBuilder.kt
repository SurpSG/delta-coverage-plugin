package io.github.surpsg.deltacoverage.report.intellij.report

import com.intellij.rt.coverage.report.Reporter
import io.github.surpsg.deltacoverage.config.ReportsConfig
import java.io.File

internal abstract class ReportBuilder(
    private val reporter: Reporter,
    private val reportsConfig: ReportsConfig
) {

    protected abstract val reportOutputFilePath: String

    fun buildReport() {
        val reportPath = File(reportsConfig.baseReportDir).resolve(reportOutputFilePath)
        buildReport(reportPath, reporter)
    }

    protected abstract fun buildReport(reportPath: File, reporter: Reporter)

}

package io.github.surpsg.deltacoverage.report.jacoco.csv

import io.github.surpsg.deltacoverage.report.ReportBound
import io.github.surpsg.deltacoverage.report.ReportType
import io.github.surpsg.deltacoverage.report.textual.TextualReportFacade
import java.io.ByteArrayOutputStream
import java.io.OutputStream

internal class TextualReportOutputStream(
    private val reportType: ReportType,
    private val reportBound: ReportBound,
    private val outputStream: OutputStream,
) : OutputStream() {

    private val byteArrayOutputStream = ByteArrayOutputStream()

    override fun write(b: Int) = byteArrayOutputStream.write(b)

    override fun write(b: ByteArray, off: Int, len: Int) = super.write(b, off, len)

    override fun close() {
        outputStream.use { os ->
            val buildContext = TextualReportFacade.BuildContext {
                coverageDataProvider = CsvSourceRawCoverageDataProvider(byteArrayOutputStream.toByteArray())
                reportType = this@TextualReportOutputStream.reportType
                reportBound = this@TextualReportOutputStream.reportBound
                outputStream = os
                shrinkLongClassName = (reportType == ReportType.CONSOLE)
            }
            TextualReportFacade.generateReport(buildContext)
        }
        super.close()
    }
}

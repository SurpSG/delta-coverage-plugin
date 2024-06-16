package io.github.surpsg.deltacoverage.report.textual

import io.github.surpsg.deltacoverage.lib.kotlin.applyIf
import io.github.surpsg.deltacoverage.report.ReportBound
import io.github.surpsg.deltacoverage.report.ReportType
import io.github.surpsg.deltacoverage.report.textual.TextualReportRenderer.Context
import java.io.OutputStream

internal object TextualReportFacade {

    private const val MAX_CLASS_COLUMN_LENGTH = 100
    private const val SHRINK_PLACEHOLDER = "..."

    private const val PERCENT_MULTIPLIER = 100

    private const val NA_VALUE = ""

    private const val CLASS_H = "Class"
    private const val LINES_H = "Lines"
    private const val BRANCHES_H = "Branches"
    private val HEADERS = listOf(CLASS_H, LINES_H, BRANCHES_H)

    fun generateReport(
        buildContext: BuildContext,
    ) {
        val rawCoverageData: List<RawCoverageData> = buildContext.coverageDataProvider.obtainData()
        val coverageDataValues: List<List<String>> =
            rawCoverageData
                .sortedByDescending { it.linesRatio }
                .asSequence()
                .map { it.toValuesCollection(buildContext) }
                .toList()

        TextualReportRendererFactory.getBy(buildContext.reportType).render(
            Context {
                output = buildContext.outputStream
                title = buildTitle(buildContext)
                headers = HEADERS
                footer = rawCoverageData.computeTotal().toValuesCollection(buildContext)
                rows = coverageDataValues
            }
        )
    }

    private fun RawCoverageData.toValuesCollection(
        buildContext: BuildContext,
    ): List<String> = HEADERS.map { header ->
        when (header) {
            LINES_H -> linesRatio.formatToPercentage()

            CLASS_H -> aClass.applyIf(buildContext.shrinkLongClassName) {
                shrinkClassName(MAX_CLASS_COLUMN_LENGTH)
            }

            BRANCHES_H -> {
                if (branchesTotal == 0) {
                    NA_VALUE
                } else {
                    branchesRatio.formatToPercentage()
                }
            }

            else -> error("Unknown header: $header")
        }
    }

    private fun List<RawCoverageData>.computeTotal(): RawCoverageData = fold(
        RawCoverageData.newBlank { aClass = "Total" },
        RawCoverageData::merge,
    )

    private fun Double.formatToPercentage(): String {
        val percents = this.toFloat() * PERCENT_MULTIPLIER
        val pattern = if (percents % 1 == 0.0f) {
            "%.0f"
        } else {
            "%.2f"
        }
        return pattern.format(percents) + '%'
    }

    private fun String.shrinkClassName(maxLength: Int): String {
        return if (length > maxLength) {
            val startIndex = length - maxLength + SHRINK_PLACEHOLDER.length
            val keepRange = startIndex until length
            SHRINK_PLACEHOLDER + substring(keepRange)
        } else {
            this
        }
    }

    private fun buildTitle(buildContext: BuildContext): String = when (buildContext.reportBound) {
        ReportBound.DELTA_REPORT -> "Delta Coverage Stats"
        ReportBound.FULL_REPORT -> "Total Coverage Stats"
    }

    internal class BuildContext private constructor(
        val reportType: ReportType,
        val reportBound: ReportBound,
        val coverageDataProvider: RawCoverageDataProvider,
        val outputStream: OutputStream,
        val shrinkLongClassName: Boolean,
    ) {

        init {
            val supportedTypes = setOf(ReportType.CONSOLE, ReportType.MARKDOWN)
            require(
                reportType in supportedTypes
            ) {
                "Supports only $supportedTypes types"
            }
        }

        class Builder {
            lateinit var reportType: ReportType
            lateinit var reportBound: ReportBound
            lateinit var coverageDataProvider: RawCoverageDataProvider
            lateinit var outputStream: OutputStream
            var shrinkLongClassName: Boolean = false

            fun build() = BuildContext(
                reportType,
                reportBound,
                coverageDataProvider,
                outputStream,
                shrinkLongClassName
            )
        }

        companion object {
            operator fun invoke(block: Builder.() -> Unit): BuildContext = Builder().apply(block).build()
        }
    }
}

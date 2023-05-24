package io.github.surpsg.deltacoverage.report.jacoco

import io.github.surpsg.deltacoverage.report.ReportContext
import io.github.surpsg.deltacoverage.report.DeltaReportGeneratorFacade
import io.github.surpsg.deltacoverage.report.jacoco.analyzable.AnalyzableReport
import io.github.surpsg.deltacoverage.report.jacoco.analyzable.analyzableReportFactory
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IBundleCoverage
import org.jacoco.core.analysis.ICoverageVisitor
import org.jacoco.core.tools.ExecFileLoader
import org.jacoco.report.DirectorySourceFileLocator
import org.jacoco.report.ISourceFileLocator
import org.jacoco.report.MultiSourceFileLocator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

internal class JacocoDeltaReportGeneratorFacade(
    reportContext: ReportContext
) : DeltaReportGeneratorFacade(reportContext) {

    override fun generateReport(): DeltaReportGeneratorFacade {
        val analyzableReports: Set<AnalyzableReport> = analyzableReportFactory(reportContext)

        val execFileLoader = loadExecFiles()
        analyzableReports.forEach {
            create(execFileLoader, it)
        }

        return this
    }

    private fun loadExecFiles(): ExecFileLoader {
        val execFileLoader = ExecFileLoader()
        reportContext.binaryCoverageFiles.forEach {
            log.debug("Loading exec data: {}", it)
            try {
                execFileLoader.load(it)
            } catch (e: IOException) {
                throw RuntimeException("Cannot load coverage data from file: $it", e)
            }
        }
        return execFileLoader
    }

    private fun create(execFileLoader: ExecFileLoader, analyzableReport: AnalyzableReport) {
        val bundleCoverage = analyzeStructure { coverageVisitor ->
            analyzableReport.buildAnalyzer(execFileLoader.executionDataStore, coverageVisitor)
        }

        analyzableReport.buildVisitor().run {
            visitInfo(
                execFileLoader.sessionInfoStore.infos,
                execFileLoader.executionDataStore.contents
            )

            visitBundle(
                bundleCoverage,
                createSourcesLocator()
            )

            visitEnd()
        }
    }

    private fun analyzeStructure(
        createAnalyzer: (ICoverageVisitor) -> Analyzer
    ): IBundleCoverage {
        CoverageBuilder().let { builder ->

            val analyzer = createAnalyzer(builder)

            reportContext.classFiles.forEach { analyzer.analyzeAll(it) }

            return builder.getBundle(reportContext.deltaCoverageConfig.reportName)
        }
    }

    private fun createSourcesLocator(): ISourceFileLocator {
        return reportContext.srcFiles.asSequence()
            .map {
                DirectorySourceFileLocator(it, "utf-8", DEFAULT_TAB_WIDTH)
            }
            .fold(MultiSourceFileLocator(DEFAULT_TAB_WIDTH)) { accumulator, sourceLocator ->
                accumulator.apply {
                    add(sourceLocator)
                }
            }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(JacocoDeltaReportGeneratorFacade::class.java)

        const val DEFAULT_TAB_WIDTH = 4
    }
}

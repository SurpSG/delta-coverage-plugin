package io.github.surpsg.deltacoverage.report

import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.diff.DiffSource
import io.github.surpsg.deltacoverage.diff.diffSourceFactory
import io.github.surpsg.deltacoverage.report.analyzable.AnalyzableReport
import io.github.surpsg.deltacoverage.report.analyzable.analyzableReportFactory
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IBundleCoverage
import org.jacoco.core.analysis.ICoverageVisitor
import org.jacoco.core.tools.ExecFileLoader
import org.jacoco.report.DirectorySourceFileLocator
import org.jacoco.report.ISourceFileLocator
import org.jacoco.report.MultiSourceFileLocator
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class ReportGenerator(
    projectRoot: File,
    private val deltaCoverageConfig: DeltaCoverageConfig
) {
    private val jacocoExec: Set<File> = deltaCoverageConfig.binaryCoverageFiles.filter(File::exists).toSet()
    private val classesSources: Set<File> = deltaCoverageConfig.classFiles.filter(File::exists).toSet()
    private val src: Set<File> = deltaCoverageConfig.sourceFiles.filter(File::exists).toSet()

    private val diffSource: DiffSource = diffSourceFactory(projectRoot, deltaCoverageConfig.diffSourceConfig)
    private val analyzableReports: Set<AnalyzableReport> = analyzableReportFactory(deltaCoverageConfig, diffSource)

    fun saveDiffToDir(dir: File) = diffSource.saveDiffTo(dir)

    fun create() {
        val execFileLoader = loadExecFiles()

        analyzableReports.forEach {
            create(execFileLoader, it)
        }
    }

    private fun loadExecFiles(): ExecFileLoader {
        val execFileLoader = ExecFileLoader()
        jacocoExec.forEach {
            log.debug("Loading exec data $it")
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

            classesSources.forEach { analyzer.analyzeAll(it) }

            return builder.getBundle(deltaCoverageConfig.reportName)
        }
    }

    private fun createSourcesLocator(): ISourceFileLocator {
        return src.asSequence()
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
        val log = LoggerFactory.getLogger(ReportGenerator::class.java)

        const val DEFAULT_TAB_WIDTH = 4
    }
}

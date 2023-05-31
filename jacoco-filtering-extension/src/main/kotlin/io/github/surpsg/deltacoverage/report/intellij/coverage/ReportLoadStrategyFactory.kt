package io.github.surpsg.deltacoverage.report.intellij.coverage

import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.report.ReportLoadStrategy
import com.intellij.rt.coverage.report.data.BinaryReport
import com.intellij.rt.coverage.report.data.Module
import io.github.surpsg.deltacoverage.report.ReportContext

object ReportLoadStrategyFactory {

    fun buildReportLoadStrategy(reportContext: ReportContext): ReportLoadStrategy {
        val binaryReports: List<BinaryReport> = buildBinaryReports(reportContext)
        val modules: List<Module> = buildModules(reportContext)

        val filterProjectData: ProjectData = IntellijDeltaCoverageLoader.getDeltaProjectData(
            binaryReports,
            modules,
            reportContext.codeUpdateInfo
        )

        return PreloadedCoverageReportLoadStrategy(filterProjectData, binaryReports, modules)
    }

    private fun buildBinaryReports(reportContext: ReportContext): List<BinaryReport> {
        return reportContext.deltaCoverageConfig.coverageBinaryFiles.map { binaryCoverageFile ->
            BinaryReport(binaryCoverageFile, null)
        }
    }

    private fun buildModules(reportContext: ReportContext): List<Module> {
        return listOf(
            Module(
                reportContext.deltaCoverageConfig.classFiles.toList(),
                reportContext.deltaCoverageConfig.sourceFiles.toList()
            )
        )
    }

    private class PreloadedCoverageReportLoadStrategy(
        private val coverageData: ProjectData,
        binaryReports: List<BinaryReport>,
        modules: List<Module>
    ) : ReportLoadStrategy(binaryReports, modules) {

        override fun loadProjectData(): ProjectData = coverageData
    }

}

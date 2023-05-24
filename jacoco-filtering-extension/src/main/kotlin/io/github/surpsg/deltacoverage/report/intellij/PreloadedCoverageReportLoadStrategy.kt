package io.github.surpsg.deltacoverage.report.intellij

import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.report.ReportLoadStrategy
import com.intellij.rt.coverage.report.data.BinaryReport
import com.intellij.rt.coverage.report.data.Module

internal class PreloadedCoverageReportLoadStrategy(
    private val coverageData: ProjectData,
    binaryReports: List<BinaryReport>,
    modules: List<Module>
) : ReportLoadStrategy(binaryReports, modules) {

    override fun loadProjectData(): ProjectData = coverageData
}

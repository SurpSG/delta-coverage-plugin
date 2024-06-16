package io.github.surpsg.deltacoverage.report.intellij.coverage

import com.intellij.rt.coverage.data.ClassData
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.report.XMLCoverageReport
import io.github.surpsg.deltacoverage.report.textual.RawCoverageData
import io.github.surpsg.deltacoverage.report.textual.RawCoverageDataProvider

internal class IntellijRawCoverageDataProvider(
    private val projectData: ProjectData,
) : RawCoverageDataProvider {

    override fun obtainData(): List<RawCoverageData> =
        XMLCoverageReport.mapClassesToPackages(projectData, true)
            .values.asSequence()
            .flatMap { it }
            .map(::collectClassCoverageData)
            .toList()

    private fun collectClassCoverageData(classData: ClassData): RawCoverageData {
        val classCoverage = RawCoverageData.newBlank {
            aClass = classData.name
        }
        return classData.mapLinesToMethods().values
            .asSequence()
            .map(::collectMethodData)
            .fold(classCoverage, RawCoverageData::merge)
    }

    private fun collectMethodData(lines: List<LineData>): RawCoverageData =
        lines.asSequence()
            .map(::buildLineRawCoverage)
            .reduce(RawCoverageData::merge)

    private fun buildLineRawCoverage(lineData: LineData) = RawCoverageData.newBlank {
        linesTotal = 1
        linesCovered = if (lineData.hits > 0) 1 else 0

        val branchData = lineData.branchData
        branchesTotal = branchData?.totalBranches ?: 0
        branchesCovered = branchData?.coveredBranches ?: 0
    }
}

package io.github.surpsg.deltacoverage.incubator

import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.report.ReportLoadStrategy
import com.intellij.rt.coverage.report.Reporter
import com.intellij.rt.coverage.report.data.BinaryReport
import com.intellij.rt.coverage.report.data.Filters
import com.intellij.rt.coverage.report.data.Module
import io.github.surpsg.deltacoverage.diff.CodeUpdateInfo
import java.io.File

fun main() {
    val projectRoot = File("./jacoco-filtering-extension")
    val binaryReports: List<BinaryReport> = listOf(
        BinaryReport(
            File("/home/sergnat/ideaProjects/delta-coverage-gradle/jacoco-filtering-extension/build/kover/raw-reports/test.ic"),
            null
        )
    )
    val modules: List<Module> = listOf(
        Module(
            // outputRoots
            listOf(File("/home/sergnat/ideaProjects/delta-coverage-gradle/jacoco-filtering-extension/build/classes/kotlin/main")),
            // sourceRoots
            listOf(File("/home/sergnat/ideaProjects/delta-coverage-gradle/jacoco-filtering-extension/src/main/kotlin")),
        )
    )
//    val loadStrategy: ReportLoadStrategy = ReportLoadStrategy.RawReportLoadStrategy(
//        binaryReports,
//        modules,
//        Filters.EMPTY
//    )

//    val mergedProjectData: ProjectData = loadStrategy.projectData

    val filterProjectData = getProjectData(
        binaryReports,
        modules,
        CodeUpdateInfo(
            mapOf(
                "jacoco-filtering-extension/src/main/kotlin/io/github/surpsg/deltacoverage/diff/DiffSource.kt" to setOf(84, 26, 28, 87, 47)
            )
        )
    )
//
    val deltaReportLoadStrategy = DeltaReportLoadStrategy(filterProjectData, binaryReports, modules)
    Reporter(
        deltaReportLoadStrategy
//        loadStrategy
    ).createHTMLReport(
        projectRoot.resolve("./khtml"),
        "azaza",
        null
    )
}

class DeltaReportLoadStrategy(
    private val filteredProjectData: ProjectData,
    binaryReports: List<BinaryReport>,
    modules: List<Module>
) : ReportLoadStrategy(binaryReports, modules) {

    override fun loadProjectData(): ProjectData = filteredProjectData
}
//class DeltaIdeaCoverageData : IDEACoverageData() {
//
//}

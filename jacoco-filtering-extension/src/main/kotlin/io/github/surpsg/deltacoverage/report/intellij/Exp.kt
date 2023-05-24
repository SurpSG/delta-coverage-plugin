package io.github.surpsg.deltacoverage.report.intellij

import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.config.DiffSourceConfig
import io.github.surpsg.deltacoverage.config.ReportConfig
import io.github.surpsg.deltacoverage.config.ReportsConfig
import io.github.surpsg.deltacoverage.config.ViolationRuleConfig
import io.github.surpsg.deltacoverage.report.CoverageEngine
import io.github.surpsg.deltacoverage.report.DeltaReportFacadeFactory
import java.io.File

fun main() {
    val config = DeltaCoverageConfig(
        "Delta Coverage Report",
        DiffSourceConfig(file = "/home/sergnat/ideaProjects/delta-coverage-gradle/diff.patch"),
        reportsConfig = ReportsConfig(
            baseReportDir = "/home/sergnat/ideaProjects/delta-coverage-gradle/build/rep",
            html = ReportConfig(enabled = true, outputFileName = "khtml"),
            xml = ReportConfig(enabled = true, outputFileName = "kxml"),
            csv = ReportConfig(enabled = true, outputFileName = "kcsv"),
        ),
        violationRuleConfig = ViolationRuleConfig(),
        coverageBinaryFiles = setOf(File("/home/sergnat/ideaProjects/delta-coverage-gradle/jacoco-filtering-extension/build/kover/raw-reports/test.ic")),
        sourceFiles = setOf(
            File("/home/sergnat/ideaProjects/delta-coverage-gradle/jacoco-filtering-extension/src/main/kotlin"),
            File("/home/sergnat/ideaProjects/delta-coverage-gradle/delta-coverage/src/main/kotlin"),
        ),
        classFiles = setOf(
            File("/home/sergnat/ideaProjects/delta-coverage-gradle/jacoco-filtering-extension/build/classes/kotlin/main"),
            File("/home/sergnat/ideaProjects/delta-coverage-gradle/delta-coverage/build/classes/kotlin/main")
        )
    )
    val baseReportDir = File(config.reportsConfig.baseReportDir)
    DeltaReportFacadeFactory
        .buildFacade(baseReportDir, CoverageEngine.INTELLIJ, config)
        .saveDiffTo(File("./diffchyk.diff"))
        .generateReport()
}

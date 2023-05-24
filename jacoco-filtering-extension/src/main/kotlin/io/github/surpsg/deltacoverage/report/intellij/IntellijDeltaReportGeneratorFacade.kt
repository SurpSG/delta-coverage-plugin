package io.github.surpsg.deltacoverage.report.intellij

import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.report.ReportLoadStrategy
import com.intellij.rt.coverage.report.Reporter
import com.intellij.rt.coverage.report.data.BinaryReport
import com.intellij.rt.coverage.report.data.Module
import com.intellij.rt.coverage.verify.Verifier
import com.intellij.rt.coverage.verify.Verifier.BoundViolation
import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.report.DeltaReportGeneratorFacade
import io.github.surpsg.deltacoverage.report.ReportContext
import java.io.File
import java.math.BigDecimal

internal class IntellijDeltaReportGeneratorFacade(
    reportContext: ReportContext
) : DeltaReportGeneratorFacade(reportContext) {

    override fun generateReport(): DeltaReportGeneratorFacade {
        generate(reportContext.deltaCoverageConfig)

        return this
    }

    private fun generate(deltaCoverageConfig: DeltaCoverageConfig) {
        val baseReportDir = File(deltaCoverageConfig.reportsConfig.baseReportDir)

        val preloadedCoverage: PreloadedCoverageReportLoadStrategy = loadCoverage()
        Reporter(preloadedCoverage)
            .createHTMLReport(
                baseReportDir.resolve(deltaCoverageConfig.reportsConfig.html.outputFileName),
                deltaCoverageConfig.reportName,
                null
            )

        verifyByRules(preloadedCoverage)
    }

    private fun verifyByRules(loadStrategy: ReportLoadStrategy) {
        listOf(
            Verifier.Bound(
                1,
                Verifier.Counter.LINE,
                Verifier.ValueType.COVERED_RATE,
                BigDecimal.valueOf(0.5),
                BigDecimal.ZERO
            ),
            Verifier.Bound(
                2,
                Verifier.Counter.BRANCH,
                Verifier.ValueType.COVERED_RATE,
                BigDecimal.valueOf(0.9),
                BigDecimal.ZERO
            ),
            Verifier.Bound(
                3,
                Verifier.Counter.INSTRUCTION,
                Verifier.ValueType.COVERED_RATE,
                BigDecimal.valueOf(1.0),
                BigDecimal.ZERO
            ),
        ).forEach {
            verifyByRules(it, loadStrategy)
        }
    }

    private fun verifyByRules(rule: Verifier.Bound, loadStrategy: ReportLoadStrategy) {
        val violations: MutableMap<Int, BoundViolation> = mutableMapOf()

        Verifier.Target.ALL.createTargetProcessor()
            .process(loadStrategy.projectData) { name, coverage ->
                val counter: Verifier.CollectedCoverage.Counter = rule.counter.getCounter(coverage)
                val value = rule.valueType.getValue(counter) ?: return@process
                if (rule.min != null && value < rule.min) {
                    val violation: BoundViolation = violations.computeIfAbsent(rule.id) { id ->
                        BoundViolation(id)
                    }
                    violation.minViolations.add(Verifier.Violation(name, value))
                }
            }

        violations.values.forEach {
            it.minViolations.forEach {
                println("Violated ${rule.counter}: actual=${it.targetValue}, expected=${rule.min}")
            }
        }
    }

    private fun loadCoverage(): PreloadedCoverageReportLoadStrategy {
        val binaryReports: List<BinaryReport> = buildBinaryReports()
        val modules: List<Module> = buildModules()

        val filterProjectData: ProjectData = getProjectData(
            binaryReports,
            modules,
            reportContext.codeUpdateInfo
        )

        return PreloadedCoverageReportLoadStrategy(filterProjectData, binaryReports, modules)
    }

    private fun buildBinaryReports(): List<BinaryReport> {
        return reportContext.deltaCoverageConfig.coverageBinaryFiles.map { binaryCoverageFile ->
            BinaryReport(
                binaryCoverageFile,
                null
            )
        }
    }

    private fun buildModules(): List<Module> {
        return listOf(
            Module(
                reportContext.deltaCoverageConfig.classFiles.toList(),
                reportContext.deltaCoverageConfig.sourceFiles.toList()
            )
        )
    }
}

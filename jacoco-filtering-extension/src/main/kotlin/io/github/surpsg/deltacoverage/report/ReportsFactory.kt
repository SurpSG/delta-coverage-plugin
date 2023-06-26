package io.github.surpsg.deltacoverage.report

import io.github.surpsg.deltacoverage.config.CoverageEntity
import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.config.ReportsConfig
import io.github.surpsg.deltacoverage.config.CoverageRulesConfig
import io.github.surpsg.deltacoverage.diff.DiffSource
import org.jacoco.core.analysis.ICoverageNode
import org.jacoco.report.check.Limit
import org.jacoco.report.check.Rule
import java.nio.file.Paths

internal fun reportFactory(
    diffSourceConfig: DeltaCoverageConfig,
    diffSource: DiffSource
): Set<FullReport> {
    val reports: Set<Report> = diffSourceConfig.reportsConfig.toReportTypes()

    val violationRule: Rule = buildRule(diffSourceConfig.coverageRulesConfig)
    val baseReportDir = Paths.get(diffSourceConfig.reportsConfig.baseReportDir)
    val report: MutableSet<FullReport> = mutableSetOf(
        DiffReport(
            baseReportDir.resolve("deltaCoverage"),
            reports,
            diffSource,
            Violation(
                diffSourceConfig.coverageRulesConfig.failOnViolation,
                listOf(violationRule)
            )
        )
    )

    if (diffSourceConfig.reportsConfig.fullCoverageReport) {
        report += FullReport(
            baseReportDir.resolve("fullReport"),
            reports
        )
    }

    return report
}

private fun ReportsConfig.toReportTypes(): Set<Report> = sequenceOf(
    ReportType.HTML to html,
    ReportType.CSV to csv,
    ReportType.XML to xml
).filter { it.second.enabled }.map {
    Report(it.first, it.second.outputFileName)
}.toSet()

private fun buildRule(
    violationRulesOptions: CoverageRulesConfig
): Rule {
    return violationRulesOptions.entitiesRules.asSequence()
        .map { (coverageEntity, minValue) ->
            coverageEntity.toJacocoEntity() to minValue.minCoverageRatio
        }
        .filter { (_, minCoverage) ->
            minCoverage > 0.0
        }.map { (counterType, minCoverage) ->
            Limit().apply {
                setCounter(counterType.name)
                minimum = minCoverage.toString()
            }
        }.toList().let {
            Rule().apply {
                limits = it
            }
        }
}

private fun CoverageEntity.toJacocoEntity(): ICoverageNode.CounterEntity {
    return when (this) {
        CoverageEntity.INSTRUCTION -> ICoverageNode.CounterEntity.INSTRUCTION
        CoverageEntity.BRANCH -> ICoverageNode.CounterEntity.BRANCH
        CoverageEntity.LINE -> ICoverageNode.CounterEntity.LINE
    }
}

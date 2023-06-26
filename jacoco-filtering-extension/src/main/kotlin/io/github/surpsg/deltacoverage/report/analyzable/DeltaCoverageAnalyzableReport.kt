package io.github.surpsg.deltacoverage.report.analyzable

import io.github.surpsg.deltacoverage.config.CoverageRulesConfig
import io.github.surpsg.deltacoverage.diff.CodeUpdateInfo
import io.github.surpsg.deltacoverage.diff.parse.ClassFile
import io.github.surpsg.deltacoverage.diff.parse.ModifiedLinesDiffParser
import io.github.surpsg.deltacoverage.filters.ModifiedLinesFilter
import io.github.surpsg.deltacoverage.report.DiffReport
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.ICoverageVisitor
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.internal.analysis.FilteringAnalyzer
import org.jacoco.report.IReportVisitor
import org.jacoco.report.MultiReportVisitor
import org.jacoco.report.check.Rule
import org.jacoco.report.check.RulesChecker
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class DeltaCoverageAnalyzableReport(
    private val violationRuleConfig: CoverageRulesConfig,
    private val diffReport: DiffReport
) : FullCoverageAnalyzableReport(diffReport) {

    override fun buildVisitor(): IReportVisitor {
        val visitors: MutableList<IReportVisitor> = mutableListOf(super.buildVisitor())

        visitors += createViolationCheckVisitor(
            diffReport.violation.failOnViolation,
            diffReport.violation.violationRules
        )

        return MultiReportVisitor(visitors)
    }

    override fun buildAnalyzer(
        executionDataStore: ExecutionDataStore,
        coverageVisitor: ICoverageVisitor
    ): Analyzer {
        val codeUpdateInfo = obtainCodeUpdateInfo()
        val classFileFilter: (ClassFile) -> Boolean = {
            codeUpdateInfo.isInfoExists(it)
        }
        return FilteringAnalyzer(executionDataStore, coverageVisitor, classFileFilter) {
            ModifiedLinesFilter(codeUpdateInfo)
        }
    }

    private fun obtainCodeUpdateInfo(): CodeUpdateInfo {
        val changesMap = ModifiedLinesDiffParser().collectModifiedLines(
            diffReport.diffSource.pullDiff()
        )
        changesMap.forEach { (file, rows) ->
            log.debug("File $file has ${rows.size} modified lines")
        }
        return CodeUpdateInfo(changesMap)
    }

    private fun createViolationCheckVisitor(
        failOnViolation: Boolean,
        rules: List<Rule>
    ): IReportVisitor {
        val log = LoggerFactory.getLogger("ViolationRules")
        val violationsOutputResolver = ViolationsOutputResolver(violationRuleConfig)

        class CoverageRulesVisitor(
            rulesCheckerVisitor: IReportVisitor
        ) : IReportVisitor by rulesCheckerVisitor {
            override fun visitEnd() {
                val violations = violationsOutputResolver.getViolations()
                log.warn("Fail on violations: $failOnViolation. Found violations: ${violations.size}.")
                if (violations.isNotEmpty() && failOnViolation) {
                    throw Exception(violations.joinToString("\n"))
                }
            }
        }

        return RulesChecker()
            .apply { setRules(rules) }
            .createVisitor(violationsOutputResolver)
            .let { CoverageRulesVisitor(it) }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(DeltaCoverageAnalyzableReport::class.java)
    }
}

package io.github.surpsg.deltacoverage.report.analyzable

import io.github.surpsg.deltacoverage.config.CoverageEntity
import io.github.surpsg.deltacoverage.config.CoverageRulesConfig
import io.github.surpsg.deltacoverage.config.ViolationRule
import org.jacoco.core.analysis.ICoverageNode
import org.jacoco.report.check.IViolationsOutput
import org.jacoco.report.check.Limit
import org.jacoco.report.check.Rule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class ViolationsOutputResolver(
    violationRuleConfig: CoverageRulesConfig
) : IViolationsOutput {

    private val violationRules: Map<CoverageEntity, ViolationRule> by lazy { violationRuleConfig.entitiesRules }

    private val foundViolations: MutableList<String> = mutableListOf()

    fun getViolations(): List<String> = foundViolations

    override fun onViolation(node: ICoverageNode, rule: Rule, limit: Limit, message: String) {
        log.debug("New violation: $message")

        val violationResolveContext: ViolationResolveContext = buildViolationResolveContext(limit.entity, node)
        if (violationResolveContext.isIgnoredByThreshold()) {
            log.info(
                "Coverage violation of {} was ignored because threshold={} but total={}",
                violationResolveContext.coverageEntity,
                violationResolveContext.thresholdCount,
                violationResolveContext.totalCount
            )
        } else {
            foundViolations += message
        }
    }

    private fun buildViolationResolveContext(
        counterEntity: ICoverageNode.CounterEntity,
        coverage: ICoverageNode,
    ): ViolationResolveContext {
        val totalEntityCount: Int = coverage.getCounter(counterEntity).totalCount
        val coverageEntity: CoverageEntity = counterEntity.mapToCoverageEntity() ?: return NO_IGNORE_VIOLATION_CONTEXT

        return violationRules[coverageEntity]
            ?.entityCountThreshold
            ?.let { threshold -> ViolationResolveContext(coverageEntity, threshold, totalEntityCount) }
            ?: NO_IGNORE_VIOLATION_CONTEXT
    }

    private fun ICoverageNode.CounterEntity.mapToCoverageEntity(): CoverageEntity? = when (this) {
        ICoverageNode.CounterEntity.INSTRUCTION -> CoverageEntity.INSTRUCTION
        ICoverageNode.CounterEntity.BRANCH -> CoverageEntity.BRANCH
        ICoverageNode.CounterEntity.LINE -> CoverageEntity.LINE
        else -> null
    }

    private open class ViolationResolveContext(
        val coverageEntity: CoverageEntity?,
        val thresholdCount: Int,
        val totalCount: Int
    ) {
        open fun isIgnoredByThreshold(): Boolean {
            return totalCount < thresholdCount
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ViolationsOutputResolver::class.java)

        val NO_IGNORE_VIOLATION_CONTEXT = object : ViolationResolveContext(null, -1, -1) {
            override fun isIgnoredByThreshold() = false
        }
    }
}

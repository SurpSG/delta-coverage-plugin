package io.github.surpsg.deltacoverage.report.jacoco

import io.github.surpsg.deltacoverage.config.ViolationRuleConfig
import org.jacoco.core.analysis.ICoverageNode
import org.jacoco.report.check.Limit
import org.jacoco.report.check.Rule

internal fun buildRule(
    violationRulesOptions: ViolationRuleConfig
): Rule {
    return sequenceOf(
        ICoverageNode.CounterEntity.INSTRUCTION to violationRulesOptions.minInstructions,
        ICoverageNode.CounterEntity.BRANCH to violationRulesOptions.minBranches,
        ICoverageNode.CounterEntity.LINE to violationRulesOptions.minLines
    ).filter {
        it.second > 0.0
    }.map {
        Limit().apply {
            setCounter(it.first.name)
            minimum = it.second.toString()
        }
    }.toList().let {
        Rule().apply {
            limits = it
        }
    }
}

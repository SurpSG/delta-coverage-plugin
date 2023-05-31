package io.github.surpsg.deltacoverage.report.intellij.verifier

import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.verify.Verifier
import io.github.surpsg.deltacoverage.config.ViolationRuleConfig
import java.math.BigDecimal

internal object VerifierFactory {

    fun buildVerifiers(
        projectData: ProjectData,
        violationRuleConfig: ViolationRuleConfig,
    ): Iterable<CoverageVerifier> {
        return sequenceOf(
            Verifier.Counter.INSTRUCTION to violationRuleConfig.minInstructions,
            Verifier.Counter.BRANCH to violationRuleConfig.minBranches,
            Verifier.Counter.LINE to violationRuleConfig.minLines
        )
            .filter { (_, minValue) ->
                minValue > 0.0
            }
            .mapIndexed { index, (verifierType, minValue) ->
                verifierType.buildVerifier(index, minValue)
            }
            .map { verifierBound ->
                CoverageVerifier(projectData, verifierBound)
            }
            .toList()
    }

    private fun Verifier.Counter.buildVerifier(
        id: Int,
        minValue: Double
    ): Verifier.Bound {
        return Verifier.Bound(
            id,
            this,
            Verifier.ValueType.COVERED_RATE,
            BigDecimal.valueOf(minValue),
            BigDecimal.ZERO
        )
    }

}

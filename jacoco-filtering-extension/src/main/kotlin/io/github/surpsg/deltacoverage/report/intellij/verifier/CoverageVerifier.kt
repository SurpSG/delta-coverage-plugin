package io.github.surpsg.deltacoverage.report.intellij.verifier

import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.verify.Verifier
import java.math.BigDecimal

internal class CoverageVerifier(
    private val projectData: ProjectData,
    private val rule: Verifier.Bound
) {

    fun verify(): Iterable<Violation> {
        val violations: MutableList<Violation> = mutableListOf()

        Verifier.Target.ALL.createTargetProcessor().process(projectData) { name, coverage ->
            val counter: Verifier.CollectedCoverage.Counter = rule.counter.getCounter(coverage)
            val actualValue: BigDecimal? = rule.valueType.getValue(counter)
            if (actualValue != null && rule.min != null && actualValue < rule.min) {
                violations += Violation(
                    coverageTrackType = rule.counter.name,
                    expectedMinValue = rule.min.toDouble(),
                    actualValue = actualValue.toDouble()
                )
            }
        }

        return violations
    }

    data class Violation(
        val coverageTrackType: String,
        val expectedMinValue: Double,
        val actualValue: Double,
    )

}

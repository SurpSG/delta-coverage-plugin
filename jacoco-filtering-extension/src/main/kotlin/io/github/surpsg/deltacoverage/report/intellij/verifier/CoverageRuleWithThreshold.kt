package io.github.surpsg.deltacoverage.report.intellij.verifier

import com.intellij.rt.coverage.verify.Verifier
import io.github.surpsg.deltacoverage.config.CoverageEntity
import java.math.BigDecimal

internal class CoverageRuleWithThreshold(
    val id: Int,
    val counter: CoverageEntity,
    val valueType: Verifier.ValueType,
    val min: BigDecimal,
    val threshold: Int?
)

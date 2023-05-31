package io.github.surpsg.deltacoverage.report.intellij.verifier

import com.intellij.rt.coverage.data.ProjectData
import io.github.surpsg.deltacoverage.config.ViolationRuleConfig
import io.github.surpsg.deltacoverage.exception.CoverageViolatedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object CoverageAssertion {

    private val log: Logger = LoggerFactory.getLogger(CoverageAssertion::class.java)

    fun verify(
        projectData: ProjectData,
        violationRuleConfig: ViolationRuleConfig
    ) {
        val violations: List<CoverageVerifier.Violation> = VerifierFactory
            .buildVerifiers(projectData, violationRuleConfig)
            .flatMap { coverageVerifier -> coverageVerifier.verify() }

        if (violationRuleConfig.failOnViolation) {
            throwIfCoverageViolated(violations)
        } else {
            printViolations(violations)
        }
    }

    private fun throwIfCoverageViolated(violations: List<CoverageVerifier.Violation>) {
        if (violations.isNotEmpty()) {
            val errorDetails: String = violations.joinToString(";\n") {
                it.buildCoverageViolatedMessage()
            }
            throw CoverageViolatedException(errorDetails)
        }
    }

    private fun printViolations(violations: List<CoverageVerifier.Violation>) {
        violations.forEach { violation ->
            log.warn(violation.buildCoverageViolatedMessage())
        }
    }

    private fun CoverageVerifier.Violation.buildCoverageViolatedMessage(): String {
        return "$coverageTrackType: expectedMin=$expectedMinValue, actual=$actualValue"
    }

}

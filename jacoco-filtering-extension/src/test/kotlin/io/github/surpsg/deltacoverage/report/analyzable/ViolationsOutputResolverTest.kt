package io.github.surpsg.deltacoverage.report.analyzable

import io.github.surpsg.deltacoverage.config.CoverageEntity
import io.github.surpsg.deltacoverage.config.CoverageRulesConfig
import io.github.surpsg.deltacoverage.config.ViolationRule
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.mockk
import org.jacoco.core.analysis.CoverageNodeImpl
import org.jacoco.core.analysis.ICounter
import org.jacoco.core.analysis.ICoverageNode
import org.jacoco.core.internal.analysis.CounterImpl
import org.jacoco.report.check.Limit
import org.jacoco.report.check.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class ViolationsOutputResolverTest {

    @Test
    fun `should return empty violation rules initially`() {
        // GIVEN
        val violationsOutputResolver = ViolationsOutputResolver(mockk())

        // WHEN
        val actualViolations: List<String> = violationsOutputResolver.getViolations()

        // THEN
        actualViolations.shouldBeEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["LINE", "INSTRUCTION", "BRANCH"])
    fun `should return violation message if rule violated`(entityName: String) {
        // GIVEN

        val violationsOutputResolver = ViolationsOutputResolver(
            CoverageRulesConfig {
                violationRules += ViolationRule {
                    minCoverageRatio = 0.1
                    coverageEntity = CoverageEntity.valueOf(entityName)
                }
            }
        )
        val node: CoverageNodeImpl = buildCoverageNode()
        val limit: Limit = ICoverageNode.CounterEntity.valueOf(entityName).buildLimit()

        // WHEN
        violationsOutputResolver.onViolation(node, Rule(), limit, COVERAGE_ERROR_MSG)

        // THEN
        assertSoftly(violationsOutputResolver.getViolations()) {
            shouldContainExactly(COVERAGE_ERROR_MSG)
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "LINE, 100, 1",
            "INSTRUCTION, 100, 100",
            "BRANCH, 1, 1",
        ]
    )
    fun `should return violation message if rule violated and total count is greater than or equal to threshold`(
        entityName: String,
        totalCount: Int,
        threshold: Int
    ) {
        // GIVEN
        val violationsOutputResolver = ViolationsOutputResolver(
            CoverageRulesConfig {
                violationRules += ViolationRule {
                    minCoverageRatio = 0.2
                    coverageEntity = CoverageEntity.valueOf(entityName)
                    entityCountThreshold = threshold
                }
            }
        )
        val coverageNode: CoverageNodeImpl = buildCoverageNode(totalCount)
        val limit: Limit = ICoverageNode.CounterEntity.valueOf(entityName).buildLimit()

        // WHEN
        violationsOutputResolver.onViolation(coverageNode, Rule(), limit, COVERAGE_ERROR_MSG)

        // THEN
        assertSoftly(violationsOutputResolver.getViolations()) {
            shouldContainExactly(COVERAGE_ERROR_MSG)
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "LINE, 1, 2",
            "INSTRUCTION, 100, 101",
            "BRANCH, 1, 100",
        ]
    )
    fun `should return empty violations if rule violated and total count does not reach threshold`(
        entityName: String,
        totalCount: Int,
        threshold: Int
    ) {
        // GIVEN
        val violationsOutputResolver = ViolationsOutputResolver(
            CoverageRulesConfig {
                violationRules += ViolationRule {
                    minCoverageRatio = 0.3
                    coverageEntity = CoverageEntity.valueOf(entityName)
                    entityCountThreshold = threshold
                }
            }
        )
        val coverageNode: CoverageNodeImpl = buildCoverageNode(totalCount)
        val limit: Limit = ICoverageNode.CounterEntity.valueOf(entityName).buildLimit()

        // WHEN
        violationsOutputResolver.onViolation(coverageNode, Rule(), limit, COVERAGE_ERROR_MSG)

        // THEN
        violationsOutputResolver.getViolations().shouldBeEmpty()
    }

    @ParameterizedTest
    @EnumSource(value = ICoverageNode.CounterEntity::class)
    fun `should not ignore violation if delta violation rule is not found by jacoco entity`(
        jacocoEntity: ICoverageNode.CounterEntity,
    ) {
        // GIVEN
        val violationsOutputResolver = ViolationsOutputResolver(CoverageRulesConfig())
        val coverageNode: CoverageNodeImpl = buildCoverageNode(1)
        val limit: Limit = jacocoEntity.buildLimit()

        // WHEN
        violationsOutputResolver.onViolation(coverageNode, Rule(), limit, COVERAGE_ERROR_MSG)

        // THEN
        violationsOutputResolver.getViolations().shouldContainExactly(COVERAGE_ERROR_MSG)
    }

    private fun buildCoverageNode(totalCount: Int = 0): CoverageNodeImpl {
        return object : CoverageNodeImpl(ICoverageNode.ElementType.CLASS, ICoverageNode.ElementType.CLASS.name) {
            override fun getInstructionCounter(): ICounter = CounterImpl.getInstance(0, totalCount)

            override fun getBranchCounter(): ICounter = CounterImpl.getInstance(0, totalCount)

            override fun getLineCounter(): ICounter = CounterImpl.getInstance(0, totalCount)
        }
    }

    private fun ICoverageNode.CounterEntity.buildLimit(): Limit {
        val thisNodeEntity = this
        return Limit().apply { setCounter(thisNodeEntity.name) }
    }

    companion object {
        const val COVERAGE_ERROR_MSG = "pure coverage test error msg"
    }
}

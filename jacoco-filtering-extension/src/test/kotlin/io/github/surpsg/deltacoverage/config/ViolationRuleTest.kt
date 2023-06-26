package io.github.surpsg.deltacoverage.config

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ViolationRuleTest {

    @ParameterizedTest
    @ValueSource(ints = [-10, -1, 0])
    fun `should throw if entity count threshold is zero or negative`(
        threshold: Int
    ) {
        shouldThrow<IllegalArgumentException> {
            ViolationRule {
                coverageEntity = CoverageEntity.INSTRUCTION
                entityCountThreshold = threshold
            }
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = [-1.0, -0.1, 1.1, 2.0])
    fun `should throw if min coverage has invalid value`(
        invalidMinCoverageRatio: Double
    ) {
        shouldThrow<IllegalArgumentException> {
            ViolationRule {
                coverageEntity = CoverageEntity.INSTRUCTION
                minCoverageRatio = invalidMinCoverageRatio
            }
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "INSTRUCTION, 1.0",
            "INSTRUCTION, 0.0",
            "INSTRUCTION, 0.5",

            "BRANCH, 1.0",
            "BRANCH, 0.0",
            "BRANCH, 0.5",

            "LINE, 1.0",
            "LINE, 0.0",
            "LINE, 0.5",
        ]
    )
    fun `should build violation rule without threshold`(
        expectedCoverageEntity: CoverageEntity,
        expectedMinCoverage: Double
    ) {
        // WHEN
        val actualRule = ViolationRule {
            coverageEntity = expectedCoverageEntity
            minCoverageRatio = expectedMinCoverage
        }

        // THEN
        assertSoftly(actualRule) {
            coverageEntity shouldBe expectedCoverageEntity
            minCoverageRatio shouldBe expectedMinCoverage
            entityCountThreshold.shouldBeNull()
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "INSTRUCTION, 1",
            "INSTRUCTION, 2",
            "INSTRUCTION, 10",
            "INSTRUCTION, 100",

            "BRANCH, 1",
            "BRANCH, 2",
            "BRANCH, 10",
            "BRANCH, 100",

            "LINE, 1",
            "LINE, 2",
            "LINE, 10",
            "LINE, 100",
        ]
    )
    fun `should build violation rule with threshold`(
        expectedCoverageEntity: CoverageEntity,
        expectedEntityCountThreshold: Int
    ) {
        // WHEN
        val actualRule = ViolationRule {
            coverageEntity = expectedCoverageEntity
            entityCountThreshold = expectedEntityCountThreshold
        }

        // THEN
        assertSoftly(actualRule) {
            coverageEntity shouldBe expectedCoverageEntity
            minCoverageRatio shouldBe 0
            entityCountThreshold.shouldNotBeNull().shouldBeEqualComparingTo(expectedEntityCountThreshold)
        }
    }
}

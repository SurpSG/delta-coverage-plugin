package io.github.surpsg.deltacoverage.config

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class CoverageRulesConfigTest {

    @Test
    fun `should build coverage rules with defaults`() {
        // WHEN
        val actual = CoverageRulesConfig {}

        // THEN
        assertSoftly(actual) {
            entitiesRules.values.shouldBeEmpty()
            failOnViolation.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should build coverage rules with fail on violation value`(
        expectedFailOnViolation: Boolean
    ) {
        // WHEN
        val actual = CoverageRulesConfig {
            failOnViolation = expectedFailOnViolation
        }

        // THEN
        assertSoftly(actual) {
            entitiesRules.shouldBeEmpty()
            failOnViolation shouldBeEqualComparingTo expectedFailOnViolation
        }
    }

    @Test
    fun `should build coverage rules with violation rules`() {
        val allRules: List<ViolationRule> = CoverageEntity.values().map {
            ViolationRule { coverageEntity = it; minCoverageRatio = 0.1 }
        }

        // WHEN
        val actual = CoverageRulesConfig {
            violationRules += allRules
        }

        // THEN
        assertSoftly(actual) {
            entitiesRules.values shouldContainExactlyInAnyOrder allRules
            failOnViolation.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @EnumSource(value = CoverageEntity::class)
    fun `should return entity mapped rules with the last rule applied`(
        entity: CoverageEntity
    ) {
        // GIVEN
        val rule1 = ViolationRule { coverageEntity = entity; minCoverageRatio = 0.1 }
        val rule2 = ViolationRule { coverageEntity = entity; minCoverageRatio = 0.2 }
        val rule3 = ViolationRule { coverageEntity = entity; minCoverageRatio = 0.3 }
        val rule4 = ViolationRule { coverageEntity = entity; minCoverageRatio = 0.4 }

        val coverageRulesConfig = CoverageRulesConfig {
            violationRules += rule1
            violationRules += rule2
            violationRules += rule3
            violationRules += rule4
        }

        // WHEN
        val actual = coverageRulesConfig.entitiesRules

        // THEN
        actual shouldContainExactly mapOf(entity to rule4)
    }

    @ParameterizedTest
    @EnumSource(value = CoverageEntity::class)
    fun `should return empty rules if rule has zero min coverage ratio`(
        entity: CoverageEntity
    ) {
        // GIVEN
        val coverageRulesConfig = CoverageRulesConfig {
            violationRules += ViolationRule {
                coverageEntity = entity
                minCoverageRatio = 0.0
            }
        }

        // WHEN
        val actual = coverageRulesConfig.entitiesRules

        // THEN
        actual.shouldBeEmpty()
    }

    @ParameterizedTest
    @EnumSource(value = CoverageEntity::class)
    fun `should return empty rules if rule with zero min coverage ratio overrides non zero min coverage rule`(
        entity: CoverageEntity
    ) {
        // GIVEN
        val coverageRulesConfig = CoverageRulesConfig {
            violationRules += ViolationRule {
                coverageEntity = entity
                minCoverageRatio = 0.5
            }

            violationRules += ViolationRule {
                coverageEntity = entity
                minCoverageRatio = 0.0
            }
        }

        // WHEN
        val actual = coverageRulesConfig.entitiesRules

        // THEN
        actual.shouldBeEmpty()
    }
}

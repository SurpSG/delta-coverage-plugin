package io.github.surpsg.deltacoverage.config

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.io.File

class DeltaCoverageConfigTest {

    @Test
    fun `should build delta coverage config with defaults`() {
        // GIVEN
        val testDiffSourceConfig = mockk<DiffSourceConfig>()

        // WHEN
        val actualConfig = DeltaCoverageConfig {
            diffSourceConfig = testDiffSourceConfig
        }

        // THEN
        assertSoftly(actualConfig) {
            reportName shouldBeEqualComparingTo "delta-coverage-report"
            diffSourceConfig shouldBe testDiffSourceConfig
            reportsConfig shouldBeEqualToComparingFields ReportsConfig {}
            coverageRulesConfig shouldBeEqualToComparingFields CoverageRulesConfig {}
            binaryCoverageFiles.shouldBeEmpty()
            classFiles.shouldBeEmpty()
            sourceFiles.shouldBeEmpty()
        }
    }

    @Test
    fun `should throw if diff source config is not configured`() {
        shouldThrow<IllegalStateException> {
            DeltaCoverageConfig {}
        }
    }

    @Test
    fun `should build delta coverage config with custom properties`() {
        // GIVEN
        val expectedReportName = "report-name"
        val expectedDiffSourceConfig = mockk<DiffSourceConfig>()
        val expectedReportsConfig = mockk<ReportsConfig>()
        val expectedCoverageRulesConfig = mockk<CoverageRulesConfig>()
        val expectedBinaries = listOf(File("exec"))
        val expectedSources = listOf(File("sources"))
        val expectedClasses = listOf(File("classes"))

        // WHEN
        val actualConfig = DeltaCoverageConfig {
            reportName = expectedReportName
            diffSourceConfig = expectedDiffSourceConfig
            reportsConfig = expectedReportsConfig
            coverageRulesConfig = expectedCoverageRulesConfig
            binaryCoverageFiles += expectedBinaries
            classFiles += expectedClasses
            sourceFiles += expectedSources
        }

        // THEN
        assertSoftly(actualConfig) {
            reportName shouldBeEqualComparingTo expectedReportName
            diffSourceConfig shouldBe expectedDiffSourceConfig
            reportsConfig shouldBe expectedReportsConfig
            coverageRulesConfig shouldBe expectedCoverageRulesConfig
            binaryCoverageFiles shouldContainExactly expectedBinaries
            classFiles shouldContainExactly expectedClasses
            sourceFiles shouldContainExactly expectedSources
        }
    }
}

package io.github.surpsg.deltacoverage.config

import io.github.surpsg.deltacoverage.diff.DiffSource
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
        val expectedDiffSource = mockk<DiffSource>()

        // WHEN
        val actualConfig = DeltaCoverageConfig {
            diffSource = expectedDiffSource
        }

        // THEN
        assertSoftly(actualConfig) {
            reportName shouldBeEqualComparingTo "delta-coverage-report"
            diffSource shouldBe expectedDiffSource
            reportsConfig shouldBeEqualToComparingFields ReportsConfig {}
            coverageRulesConfig shouldBeEqualToComparingFields CoverageRulesConfig {}
            binaryCoverageFiles.shouldBeEmpty()
            classFiles.shouldBeEmpty()
            sourceFiles.shouldBeEmpty()
        }
    }

    @Test
    fun `should throw if diff source is not configured`() {
        shouldThrow<IllegalArgumentException> {
            DeltaCoverageConfig {}
        }
    }

    @Test
    fun `should build delta coverage config with custom properties`() {
        // GIVEN
        val expectedReportName = "report-name"
        val expectedDiffSource = mockk<DiffSource>()
        val expectedReportsConfig = mockk<ReportsConfig>()
        val expectedCoverageRulesConfig = mockk<CoverageRulesConfig>()
        val expectedBinaries = listOf(File("exec"))
        val expectedSources = listOf(File("sources"))
        val expectedClasses = listOf(File("classes"))

        // WHEN
        val actualConfig = DeltaCoverageConfig {
            reportName = expectedReportName
            diffSource = expectedDiffSource
            reportsConfig = expectedReportsConfig
            coverageRulesConfig = expectedCoverageRulesConfig
            binaryCoverageFiles += expectedBinaries
            classFiles += expectedClasses
            sourceFiles += expectedSources
        }

        // THEN
        assertSoftly(actualConfig) {
            reportName shouldBeEqualComparingTo expectedReportName
            diffSource shouldBe expectedDiffSource
            reportsConfig shouldBe expectedReportsConfig
            coverageRulesConfig shouldBe expectedCoverageRulesConfig
            binaryCoverageFiles shouldContainExactly expectedBinaries
            classFiles shouldContainExactly expectedClasses
            sourceFiles shouldContainExactly expectedSources
        }
    }
}

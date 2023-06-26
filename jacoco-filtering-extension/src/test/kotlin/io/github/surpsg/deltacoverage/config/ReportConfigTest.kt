package io.github.surpsg.deltacoverage.config

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.string.shouldBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ReportConfigTest {

    @Test
    fun `should build report config with defaults`() {
        val actualConfig = ReportConfig {}

        assertSoftly(actualConfig) {
            enabled.shouldBeFalse()
            outputFileName.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "true, html",
            "true, xml",
            "true, csv",
            "false, html",
            "false, xml",
            "false, csv",
        ]
    )
    fun `should build custom report config`(
        expectedReportEnabled: Boolean,
        expectedReportFileName: String,
    ) {
        val actualConfig = ReportConfig {
            enabled = expectedReportEnabled
            outputFileName = expectedReportFileName
        }

        assertSoftly(actualConfig) {
            enabled shouldBeEqualComparingTo expectedReportEnabled
            outputFileName shouldBeEqualComparingTo expectedReportFileName
        }
    }
}

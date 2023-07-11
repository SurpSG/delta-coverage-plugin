package io.github.surpsg.deltacoverage.config

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.string.shouldBeEmpty
import org.junit.jupiter.api.Test

class ReportsConfigTest {

    @Test
    fun `should build reports config with defaults`() {
        val actualConfig = ReportsConfig {}

        assertSoftly(actualConfig) {
            html shouldBeEqualToComparingFields ReportConfig {}
            xml shouldBeEqualToComparingFields ReportConfig {}
            csv shouldBeEqualToComparingFields ReportConfig {}
            baseReportDir.shouldBeEmpty()
            fullCoverageReport.shouldBeFalse()
        }
    }

    @Test
    fun `should build reports config with custom properties`() {
        // GIVEN
        val expectedBaseReportDir = "test base report"
        val expectedHtmlReportName = "test html"
        val expectedXmlReportName = "test xml"
        val expectedCsvReportName = "test csv"

        // WHEN
        val actualConfig = ReportsConfig {
            html = ReportConfig { enabled = true; outputFileName = expectedHtmlReportName }
            xml = ReportConfig { enabled = true; outputFileName = expectedXmlReportName }
            csv = ReportConfig { enabled = true; outputFileName = expectedCsvReportName }
            baseReportDir = expectedBaseReportDir
            fullCoverageReport = true
        }

        // THEN
        assertSoftly(actualConfig) {
            assertSoftly(html) {
                enabled.shouldBeTrue()
                outputFileName shouldBeEqualComparingTo expectedHtmlReportName
            }
            assertSoftly(xml) {
                enabled.shouldBeTrue()
                outputFileName shouldBeEqualComparingTo expectedXmlReportName
            }
            assertSoftly(csv) {
                enabled.shouldBeTrue()
                outputFileName shouldBeEqualComparingTo expectedCsvReportName
            }
            baseReportDir shouldBeEqualComparingTo expectedBaseReportDir
            fullCoverageReport.shouldBeTrue()
        }
    }
}

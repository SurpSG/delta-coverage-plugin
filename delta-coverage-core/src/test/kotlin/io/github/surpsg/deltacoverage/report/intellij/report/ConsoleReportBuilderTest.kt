package io.github.surpsg.deltacoverage.report.intellij.report

import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.report.Reporter
import io.github.surpsg.deltacoverage.report.ReportBound
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class ConsoleReportBuilderTest {

    @Test
    fun `should not generate report if report bound is not delta coverage`() {
        // GIVEN
        val reporter = mockk<Reporter>()
        val builder = ConsoleReportBuilder(
            reportBound = ReportBound.FULL_REPORT,
            reporter = reporter
        )

        // WHEN
        builder.buildReport()

        // THEN
        verify { reporter wasNot Called }
    }

    @Test
    fun `should generate report if report bound is delta coverage`() {
        // GIVEN
        val reporter = mockk<Reporter> {
            every { projectData } returns ProjectData()
        }
        val builder = ConsoleReportBuilder(
            reportBound = ReportBound.DELTA_REPORT,
            reporter = reporter
        )

        // WHEN
        builder.buildReport()

        // THEN
        verify(exactly = 1) {
            reporter.projectData
        }
    }
}

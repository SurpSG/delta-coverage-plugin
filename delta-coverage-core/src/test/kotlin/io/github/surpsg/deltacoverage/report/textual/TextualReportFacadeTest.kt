package io.github.surpsg.deltacoverage.report.textual

import io.github.surpsg.deltacoverage.report.ReportBound
import io.github.surpsg.deltacoverage.report.ReportType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class TextualReportFacadeTest {

    @Test
    fun `should throw if provided unsupported report type`() {
        shouldThrow<IllegalArgumentException> {
            TextualReportFacade.BuildContext {
                reportType = ReportType.XML
                reportBound = ReportBound.DELTA_REPORT
                coverageDataProvider = object : RawCoverageDataProvider {
                    override fun obtainData() = emptyList<RawCoverageData>()
                }
                outputStream = ByteArrayOutputStream()
            }
        }
    }

    @Nested
    inner class ConsoleReportTest {

        @Test
        fun `generateReport should render report`() {
            val rawCoverageData = listOf(
                RawCoverageData.newBlank {
                    aClass = "class1"
                    branchesCovered = 1
                    branchesTotal = 2
                    linesCovered = 3
                    linesTotal = 4
                },
                RawCoverageData.newBlank {
                    aClass = "class2"
                    branchesCovered = 5
                    branchesTotal = 6
                    linesCovered = 7
                    linesTotal = 8
                }
            )
            val stream = ByteArrayOutputStream()
            val buildContext = TextualReportFacade.BuildContext {
                reportType = ReportType.CONSOLE
                reportBound = ReportBound.DELTA_REPORT
                coverageDataProvider = object : RawCoverageDataProvider {
                    override fun obtainData() = rawCoverageData
                }
                outputStream = stream
            }

            // WHEN
            TextualReportFacade.generateReport(buildContext)

            // THEN
            val expectedReport = """
                +--------+--------+----------+
                | Delta Coverage Stats       |
                +--------+--------+----------+
                | Class  | Lines  | Branches |
                +--------+--------+----------+
                | class2 | 87.50% | 83.33%   |
                | class1 | 75%    | 50%      |
                +--------+--------+----------+
                | Total  | 83.33% | 75%      |
                +--------+--------+----------+
                
            """.trimIndent()
            stream.toString() shouldBe expectedReport
        }

        @Test
        fun `generateReport should render report with NA values`() {
            val rawCoverageData = listOf(
                RawCoverageData.newBlank {
                    aClass = "class1"
                    branchesCovered = 0
                    branchesTotal = 0
                    linesCovered = 0
                    linesTotal = 0
                },
            )
            val stream = ByteArrayOutputStream()
            val buildContext = TextualReportFacade.BuildContext {
                reportType = ReportType.CONSOLE
                reportBound = ReportBound.DELTA_REPORT
                coverageDataProvider = object : RawCoverageDataProvider {
                    override fun obtainData() = rawCoverageData
                }
                outputStream = stream
            }

            // WHEN
            TextualReportFacade.generateReport(buildContext)

            // THEN
            val expectedReport = """
                +--------+-------+----------+
                | Delta Coverage Stats      |
                +--------+-------+----------+
                | Class  | Lines | Branches |
                +--------+-------+----------+
                | class1 | NaN%  |          |
                +--------+-------+----------+
                | Total  | NaN%  |          |
                +--------+-------+----------+
                
            """.trimIndent()
            stream.toString() shouldBe expectedReport
        }

        @Test
        fun `generateReport should shrink class name if shrinking enabled and exceeds 100 chars threshold`() {
            val seed = "t"
            val className = seed.repeat(123)
            val expectedClass = "..." + seed.repeat(97)
            val rawCoverageData = listOf(
                RawCoverageData.newBlank {
                    aClass = className
                    linesCovered = 1
                    linesTotal = 2
                },
            )
            val stream = ByteArrayOutputStream()
            val buildContext = TextualReportFacade.BuildContext {
                reportType = ReportType.CONSOLE
                reportBound = ReportBound.DELTA_REPORT
                coverageDataProvider = object : RawCoverageDataProvider {
                    override fun obtainData() = rawCoverageData
                }
                outputStream = stream
                shrinkLongClassName = true
            }

            // WHEN
            TextualReportFacade.generateReport(buildContext)

            // THEN
            stream.toString() shouldContain "| $expectedClass | 50%   |"
        }
    }

    @Nested
    inner class TextualReportTest {

        @Test
        fun `generateReport should render report`() {
            val rawCoverageData = listOf(
                RawCoverageData.newBlank {
                    aClass = "class1"
                    branchesCovered = 1
                    branchesTotal = 2
                    linesCovered = 3
                    linesTotal = 4
                },
                RawCoverageData.newBlank {
                    aClass = "class2"
                    branchesCovered = 5
                    branchesTotal = 6
                    linesCovered = 7
                    linesTotal = 8
                }
            )
            val stream = ByteArrayOutputStream()
            val buildContext = TextualReportFacade.BuildContext {
                reportType = ReportType.MARKDOWN
                reportBound = ReportBound.DELTA_REPORT
                coverageDataProvider = object : RawCoverageDataProvider {
                    override fun obtainData() = rawCoverageData
                }
                outputStream = stream
            }

            // WHEN
            TextualReportFacade.generateReport(buildContext)

            // THEN
            val expectedReport = """
            # Delta Coverage Stats
            
            | Class  | Lines  | Branches |
            |--------|--------|----------|
            | class2 | 87.50% | 83.33%   |
            | class1 | 75%    | 50%      |
            | Total  | 83.33% | 75%      |
            
        """.trimIndent()
            stream.toString() shouldBe expectedReport
        }

        @Test
        fun `generateReport should render report with NA values`() {
            val rawCoverageData = listOf(
                RawCoverageData.newBlank {
                    aClass = "class1"
                    branchesCovered = 0
                    branchesTotal = 0
                    linesCovered = 0
                    linesTotal = 0
                },
            )
            val stream = ByteArrayOutputStream()
            val buildContext = TextualReportFacade.BuildContext {
                reportType = ReportType.MARKDOWN
                reportBound = ReportBound.DELTA_REPORT
                coverageDataProvider = object : RawCoverageDataProvider {
                    override fun obtainData() = rawCoverageData
                }
                outputStream = stream
            }

            // WHEN
            TextualReportFacade.generateReport(buildContext)

            // THEN
            val expectedReport = """
            # Delta Coverage Stats
            
            | Class  | Lines | Branches |
            |--------|-------|----------|
            | class1 | NaN%  |          |
            | Total  | NaN%  |          |
            
        """.trimIndent()
            stream.toString() shouldBe expectedReport
        }

        @Test
        fun `generateReport should not shrink class name if shrinking is disabled`() {
            val seed = "t"
            val className = seed.repeat(123)
            val rawCoverageData = listOf(
                RawCoverageData.newBlank {
                    aClass = className
                    linesCovered = 1
                    linesTotal = 2
                },
            )
            val stream = ByteArrayOutputStream()
            val buildContext = TextualReportFacade.BuildContext {
                reportType = ReportType.MARKDOWN
                reportBound = ReportBound.DELTA_REPORT
                coverageDataProvider = object : RawCoverageDataProvider {
                    override fun obtainData() = rawCoverageData
                }
                outputStream = stream
                shrinkLongClassName = false
            }

            // WHEN
            TextualReportFacade.generateReport(buildContext)

            // THEN
            stream.toString() shouldContain "| $className | 50%   |"
        }
    }
}

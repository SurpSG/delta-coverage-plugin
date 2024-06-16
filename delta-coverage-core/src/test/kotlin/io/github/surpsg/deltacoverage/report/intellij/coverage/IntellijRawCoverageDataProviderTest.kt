package io.github.surpsg.deltacoverage.report.intellij.coverage

import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import io.github.surpsg.deltacoverage.report.textual.RawCoverageData
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


class IntellijRawCoverageDataProviderTest {

    @Test
    fun `should return empty list when no data`() {
        // GIVEN
        val provider = IntellijRawCoverageDataProvider(ProjectData())

        // WHEN
        val actualData = provider.obtainData()

        // THEN
        actualData.shouldBeEmpty()
    }

    @Test
    fun `should return list with single class coverage`() {
        // GIVEN
        val className = "com.example.Class1"
        val sourceName = "Class1.java"
        val provider = IntellijRawCoverageDataProvider(
            ProjectData().apply {
                getOrCreateClassData(className).apply {
                    source = sourceName

                    setLines(
                        arrayOf(LineData(1, ""))
                    )
                }
            }
        )

        // WHEN
        val actualData: List<RawCoverageData> = provider.obtainData()

        // THEN
        assertSoftly(actualData) {
            size shouldBe 1
            first() shouldBeEqualToComparingFields RawCoverageData {
                aClass = className
                branchesTotal = 0
                branchesCovered = 0
                linesCovered = 0
                linesTotal = 1
            }
        }
    }
}

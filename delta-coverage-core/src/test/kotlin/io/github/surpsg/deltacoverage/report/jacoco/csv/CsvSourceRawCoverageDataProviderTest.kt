package io.github.surpsg.deltacoverage.report.jacoco.csv

import io.github.surpsg.deltacoverage.report.textual.RawCoverageData
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import org.junit.jupiter.api.Test

class CsvSourceRawCoverageDataProviderTest {

    @Test
    fun `obtainData should return list of parsed data if input has values`() {
        // GIVEN
        val csvBytes = ("GROUP,PACKAGE,CLASS," +
                "INSTRUCTION_MISSED,INSTRUCTION_COVERED," +
                "BRANCH_MISSED,BRANCH_COVERED," +
                "LINE_MISSED,LINE_COVERED," +
                "COMPLEXITY_MISSED,COMPLEXITY_COVERED," +
                "METHOD_MISSED,METHOD_COVERED\n" +
                "group,package,class,1,2,3,4,5,6,7,8,9,10").toByteArray()
        val provider = CsvSourceRawCoverageDataProvider(csvBytes)

        // WHEN
        val actualData = provider.obtainData()

        // THEN
        assertSoftly(actualData) {
            shouldHaveSize(1)
            first() shouldBeEqualToComparingFields RawCoverageData {
                aClass = "package.class"

                branchesTotal = 7
                branchesCovered = 4

                linesTotal = 11
                linesCovered = 6
            }
        }
    }

    @Test
    fun `obtainData should return empty list if input is empty`() {
        // GIVEN
        val csvBytes = ("GROUP,PACKAGE,CLASS," +
                "INSTRUCTION_MISSED,INSTRUCTION_COVERED," +
                "BRANCH_MISSED,BRANCH_COVERED," +
                "LINE_MISSED,LINE_COVERED," +
                "COMPLEXITY_MISSED,COMPLEXITY_COVERED," +
                "METHOD_MISSED,METHOD_COVERED").toByteArray()
        val provider = CsvSourceRawCoverageDataProvider(csvBytes)

        // WHEN
        val actualData = provider.obtainData()

        // THEN
        actualData.shouldBeEmpty()
    }
}

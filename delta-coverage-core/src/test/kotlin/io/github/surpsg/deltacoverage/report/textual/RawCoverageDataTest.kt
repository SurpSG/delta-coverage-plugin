package io.github.surpsg.deltacoverage.report.textual

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RawCoverageDataTest {

    @ParameterizedTest
    @CsvSource(
        "5, 10, 0.5",
        "5, 20, 0.25",
        "0, 10, 0.0",
        "0, 0, NaN",
    )
    fun `should return correct ration for branches`(
        covered: Int,
        total: Int,
        expected: Double,
    ) {
        // GIVEN
        val data = RawCoverageData.newBlank {
            branchesCovered = covered
            branchesTotal = total
        }

        // WHEN // THEN
        data.branchesRatio shouldBe expected
    }

    @ParameterizedTest
    @CsvSource(
        "10, 100, 0.1",
        "2, 8, 0.25",
        "7, 10, 0.7",
    )
    fun `should return correct ration for lines`(
        covered: Int,
        total: Int,
        expected: Double,
    ) {
        // GIVEN
        val data = RawCoverageData.newBlank {
            branchesCovered = covered
            branchesTotal = total
        }

        // WHEN // THEN
        data.branchesRatio shouldBe expected
    }

    @Test
    fun `merge should correctly merge two RawCoverageData instances`() {
        // GIVEN
        val data1 = RawCoverageData {
            aClass = "class1"
            branchesCovered = 5
            branchesTotal = 10
            linesCovered = 5
            linesTotal = 10
        }

        val data2 = RawCoverageData {
            aClass = "class2"
            branchesCovered = 5
            branchesTotal = 10
            linesCovered = 5
            linesTotal = 10
        }

        // WHEN
        val mergedData = data1.merge(data2)

        // THEN
        mergedData shouldBeEqualToComparingFields RawCoverageData {
            aClass = "class1"
            branchesCovered = 10
            branchesTotal = 20
            linesCovered = 10
            linesTotal = 20
        }
    }

    @ParameterizedTest
    @CsvSource(
        nullValues = ["null"],
        value = [
            "null, 1, 1, 1",
            "1, null, 1, 1",
            "1, 1, null, 1",
            "1, 1, 1, null",
        ]
    )
    fun `should throw if fields not set`(
        branchesCovered: Int?,
        branchesTotal: Int?,
        linesCovered: Int?,
        linesTotal: Int?,
    ) {

        shouldThrow<IllegalArgumentException> {
            RawCoverageData {
                aClass = "class"
                this.branchesCovered = branchesCovered
                this.branchesTotal = branchesTotal
                this.linesCovered = linesCovered
                this.linesTotal = linesTotal
            }
        }
    }
}

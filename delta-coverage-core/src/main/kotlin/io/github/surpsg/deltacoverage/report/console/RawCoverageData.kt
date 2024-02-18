package io.github.surpsg.deltacoverage.report.console

internal class RawCoverageData private constructor(
    val source: String,
    val aClass: String,
    val branchesCovered: Int,
    val branchesTotal: Int,
    val linesCovered: Int,
    val linesTotal: Int,
) {

    val branchesRatio: Double
        get() = branchesCovered ratioBy branchesTotal

    val linesRatio: Double
        get() = linesCovered ratioBy linesTotal

    private infix fun Int.ratioBy(base: Int): Double = this.toDouble() / base

    fun merge(other: RawCoverageData) = this.let { thisData ->
        RawCoverageData {
            group = thisData.source
            aClass = thisData.aClass

            branchesCovered = thisData.branchesCovered + other.branchesCovered
            branchesTotal = thisData.branchesTotal + other.branchesTotal

            linesCovered = thisData.linesCovered + other.linesCovered
            linesTotal = thisData.linesTotal + other.linesTotal
        }
    }

    internal class Builder {
        lateinit var group: String
        lateinit var aClass: String

        var branchesCovered: Int? = null
        var branchesTotal: Int? = null

        var linesCovered: Int? = null
        var linesTotal: Int? = null

        fun build() = RawCoverageData(
            source = group,
            aClass = aClass,

            branchesCovered = requireNotNull(branchesCovered),
            branchesTotal = requireNotNull(branchesTotal),

            linesCovered = requireNotNull(linesCovered),
            linesTotal = requireNotNull(linesTotal),
        )
    }

    companion object {

        operator fun invoke(initialize: Builder.() -> Unit) =
            Builder().apply(initialize).build()

        fun newBlank(
            customize: Builder.() -> Unit = {}
        ) = RawCoverageData {
            group = ""
            aClass = ""
            branchesCovered = 0
            branchesTotal = 0
            linesCovered = 0
            linesTotal = 0

            customize()
        }
    }
}

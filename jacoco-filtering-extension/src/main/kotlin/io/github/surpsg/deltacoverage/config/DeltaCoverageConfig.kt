package io.github.surpsg.deltacoverage.config

import java.io.File

@DslMarker
internal annotation class DeltaCoverageConfigMarker

class DiffSourceConfig private constructor(
    val file: String = "",
    val url: String = "",
    val diffBase: String = ""
) {

    init {
        val initializedCount = sequenceOf(file, url, diffBase).filter { it.isNotBlank() }.count()
        require(initializedCount == 1) {
            "Required single diff source initialized but was: $this"
        }
    }

    override fun toString(): String {
        return "DiffSourceConfig(file='$file', url='$url', diffBase='$diffBase')"
    }

    @DeltaCoverageConfigMarker
    class Builder internal constructor() {
        var file: String = ""
        var url: String = ""
        var diffBase: String = ""

        internal fun build(): DiffSourceConfig = DiffSourceConfig(file, url, diffBase)
    }

    companion object {

        operator fun invoke(customize: Builder.() -> Unit): DiffSourceConfig =
            Builder().apply(customize).build()
    }
}

class CoverageRulesConfig private constructor(
    private val violationRules: List<ViolationRule>,
    val failOnViolation: Boolean,
) {

    /**
     * Returns violation rules map associated with coverage entity.
     * A rule is ignored if min coverage ration is not greater than zero.
     * If multiple rules with the same coverage entity exists then the latest is chosen.
     *
     * @return coverage entity to its violation rule map.
     */
    val entitiesRules: Map<CoverageEntity, ViolationRule>
        get() = violationRules.reversed()
            .asSequence()
            .distinct()
            .filter { it.minCoverageRatio > 0.0 }
            .associateBy { it.coverageEntity }

    override fun toString(): String {
        return "CoverageRulesConfig(violationRules=${entitiesRules.values}, failOnViolation=$failOnViolation)"
    }

    @DeltaCoverageConfigMarker
    class Builder internal constructor() {
        var violationRules: MutableList<ViolationRule> = mutableListOf()
        var failOnViolation: Boolean = false

        fun build(): CoverageRulesConfig = CoverageRulesConfig(
            violationRules.toList(),
            failOnViolation
        )
    }

    companion object {

        operator fun invoke(builder: Builder.() -> Unit = {}): CoverageRulesConfig = Builder().apply(builder).build()
    }
}

class ViolationRule private constructor(
    val coverageEntity: CoverageEntity,
    val minCoverageRatio: Double,
    val entityCountThreshold: Int?
) {

    init {
        require(entityCountThreshold == null || entityCountThreshold > 0) {
            "$coverageEntity count threshold must be null or greater than zero but was $entityCountThreshold."
        }
        require(minCoverageRatio in 0.0..1.0) {
            "minCoverageRatio must be in range 1..0 but was $minCoverageRatio."
        }
    }

    override fun toString(): String {
        return "ViolationRule(coverageEntity=$coverageEntity" +
                ", minCoverageRatio=$minCoverageRatio, entityCountThreshold=$entityCountThreshold)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ViolationRule

        return coverageEntity == other.coverageEntity
    }

    override fun hashCode(): Int {
        return coverageEntity.hashCode()
    }

    @DeltaCoverageConfigMarker
    class Builder internal constructor() {
        var coverageEntity: CoverageEntity? = null
        var minCoverageRatio: Double = 0.0
        var entityCountThreshold: Int? = null

        fun build(): ViolationRule = ViolationRule(
            coverageEntity ?: error("Coverage entity was not set"),
            minCoverageRatio,
            entityCountThreshold
        )
    }

    companion object {

        operator fun invoke(customize: Builder.() -> Unit = {}): ViolationRule =
            Builder().apply(customize).build()
    }
}

enum class CoverageEntity {
    INSTRUCTION, BRANCH, LINE
}

class ReportsConfig private constructor(
    val html: ReportConfig,
    val xml: ReportConfig,
    val csv: ReportConfig,
    val baseReportDir: String,
    val fullCoverageReport: Boolean,
) {

    override fun toString(): String = "ReportsConfig(html=$html, xml=$xml, csv=$csv" +
            ", baseReportDir='$baseReportDir', fullCoverageReport=$fullCoverageReport)"

    @DeltaCoverageConfigMarker
    class Builder internal constructor() {
        var html: ReportConfig = ReportConfig {}
        var xml: ReportConfig = ReportConfig {}
        var csv: ReportConfig = ReportConfig {}
        var baseReportDir: String = ""
        var fullCoverageReport: Boolean = false

        fun build(): ReportsConfig = ReportsConfig(html, xml, csv, baseReportDir, fullCoverageReport)
    }

    companion object {

        operator fun invoke(builder: Builder.() -> Unit): ReportsConfig =
            Builder().apply(builder).build()
    }
}

class ReportConfig private constructor(
    val enabled: Boolean,
    val outputFileName: String
) {

    override fun toString(): String {
        return "ReportConfig(enabled=$enabled, outputFileName='$outputFileName')"
    }

    @DeltaCoverageConfigMarker
    class Builder internal constructor() {
        var enabled: Boolean = false
        var outputFileName: String = ""

        fun build(): ReportConfig = ReportConfig(enabled, outputFileName)
    }

    companion object {

        operator fun invoke(builder: Builder.() -> Unit): ReportConfig {
            return Builder().apply(builder).build()
        }
    }
}

@Suppress("LongParameterList")
class DeltaCoverageConfig private constructor(
    val reportName: String,
    val diffSourceConfig: DiffSourceConfig,
    val reportsConfig: ReportsConfig,
    val coverageRulesConfig: CoverageRulesConfig,
    val binaryCoverageFiles: Set<File>,
    val classFiles: Set<File>,
    val sourceFiles: Set<File>
) {

    override fun toString(): String {
        return "DeltaCoverageConfig(" +
                "reportName='$reportName'" +
                ", diffSourceConfig=$diffSourceConfig" +
                ", reportsConfig=$reportsConfig" +
                ", coverageRulesConfig=$coverageRulesConfig" +
                ", binaryCoverageFiles=$binaryCoverageFiles" +
                ", classFiles=$classFiles" +
                ", sourceFiles=$sourceFiles" +
                ")"
    }

    @DeltaCoverageConfigMarker
    class Builder internal constructor() {
        var reportName: String = "delta-coverage-report"
        var diffSourceConfig: DiffSourceConfig? = null
        var reportsConfig: ReportsConfig = ReportsConfig {}
        var coverageRulesConfig: CoverageRulesConfig = CoverageRulesConfig {}
        val binaryCoverageFiles: MutableSet<File> = mutableSetOf()
        val classFiles: MutableSet<File> = mutableSetOf()
        val sourceFiles: MutableSet<File> = mutableSetOf()

        fun build(): DeltaCoverageConfig = DeltaCoverageConfig(
            reportName,
            diffSourceConfig ?: error("'${::diffSourceConfig.name}' is not configured"),
            reportsConfig,
            coverageRulesConfig,
            binaryCoverageFiles.toSet(),
            classFiles.toSet(),
            sourceFiles.toSet()
        )
    }

    companion object {

        operator fun invoke(customize: Builder.() -> Unit): DeltaCoverageConfig {
            return Builder().apply(customize).build()
        }
    }
}


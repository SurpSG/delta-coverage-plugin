package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.CoverageEngine
import io.github.surpsg.deltacoverage.gradle.utils.booleanProperty
import io.github.surpsg.deltacoverage.gradle.utils.doubleProperty
import io.github.surpsg.deltacoverage.gradle.utils.map
import io.github.surpsg.deltacoverage.gradle.utils.new
import io.github.surpsg.deltacoverage.gradle.utils.stringProperty
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import java.nio.file.Paths
import javax.inject.Inject

open class DeltaCoverageConfiguration @Inject constructor(
    objectFactory: ObjectFactory,
) {

    @Nested
    val coverage: Coverage = objectFactory.new<Coverage>()

    @Optional
    @InputFiles
    var coverageBinaryFiles: FileCollection? = null

    @Optional
    @InputFiles
    var classesDirs: FileCollection? = null

    @Optional
    @InputFiles
    var srcDirs: FileCollection? = null

    @Input
    val excludeClasses: ListProperty<String> = objectFactory
        .listProperty(String::class.javaObjectType)
        .convention(emptyList())

    @Nested
    val diffSource: DiffSourceConfiguration = DiffSourceConfiguration(objectFactory)

    @Nested
    val reportConfiguration: ReportsConfiguration = ReportsConfiguration(objectFactory)

    @Nested
    val violationRules: ViolationRules = objectFactory.new<ViolationRules>()

    fun coverage(action: Action<in Coverage>): Unit = action.execute(coverage)

    fun reports(action: Action<in ReportsConfiguration>) {
        action.execute(reportConfiguration)
    }

    fun violationRules(action: Action<in ViolationRules>) {
        action.execute(violationRules)
    }

    fun diffSource(action: Action<in DiffSourceConfiguration>) {
        action.execute(diffSource)
    }

    override fun toString(): String {
        return "DeltaCoverageConfiguration(" +
                "coverageEngine=${coverage.engine.get()}, " +
                "coverageBinaryFiles=$coverageBinaryFiles, " +
                "classesDirs=$classesDirs, " +
                "srcDirs=$srcDirs, " +
                "excludeClasses=${excludeClasses.get()}, " +
                "diffSource=$diffSource, " +
                "reportConfiguration=$reportConfiguration, " +
                "violationRules=$violationRules)"
    }
}

open class Coverage @Inject constructor(
    objectFactory: ObjectFactory,
) {
    @Input
    val engine: Property<CoverageEngine> = objectFactory.property(CoverageEngine::class.java)
        .convention(CoverageEngine.JACOCO)

    @Input
    val autoApplyPlugin: Property<Boolean> = objectFactory.booleanProperty(true)
}

open class DiffSourceConfiguration(
    objectFactory: ObjectFactory,
) {

    @Input
    val file: Property<String> = objectFactory.stringProperty("")

    @Input
    val url: Property<String> = objectFactory.stringProperty("")

    @Nested
    val git: GitConfiguration = GitConfiguration(objectFactory)

    override fun toString(): String {
        return "DiffSourceConfiguration(file='${file.get()}', url='${url.get()}', git=$git)"
    }
}

open class GitConfiguration(
    objectFactory: ObjectFactory,
) {

    @Input
    val diffBase: Property<String> = objectFactory.stringProperty("")

    infix fun compareWith(diffBase: String) {
        this.diffBase.set(diffBase)
    }

    override fun toString(): String {
        return "GitConfiguration(diffBase='${diffBase.get()}')"
    }
}

open class ReportsConfiguration(
    objectFactory: ObjectFactory,
) {

    @Input
    val html: Property<Boolean> = objectFactory.booleanProperty(false)

    @Input
    val xml: Property<Boolean> = objectFactory.booleanProperty(false)

    @Input
    val baseReportDir: Property<String> = objectFactory.stringProperty {
        Paths.get("build", "reports").toString()
    }

    @Input
    val fullCoverageReport: Property<Boolean> = objectFactory.booleanProperty(false)

    override fun toString() = "ReportsConfiguration(" +
            "html=${html.get()}, " +
            "xml=${xml.get()}, " +
            "baseReportDir='${baseReportDir.get()}')"
}

enum class CoverageEntity {
    INSTRUCTION,
    BRANCH,
    LINE,
}

open class ViolationRules @Inject constructor(
    private val objectFactory: ObjectFactory,
) {

    @Nested
    val rules: MapProperty<CoverageEntity, ViolationRule> = objectFactory.map<CoverageEntity, ViolationRule>()

    init {
        rules.putAll(
            CoverageEntity.entries.associateWith {
                objectFactory.new<ViolationRule>()
            }
        )
    }

    @Input
    val failOnViolation: Property<Boolean> = objectFactory.booleanProperty(false)

    /**
     * Sets the minimum coverage ratio for all coverage entities.
     * Enables build failure on poor coverage.
     */
    infix fun failIfCoverageLessThan(minCoverage: Double) {
        failOnViolation.set(true)
        all { rule ->
            rule.minCoverageRatio.set(minCoverage)
        }
    }

    /**
     * Applies the given action to all [ViolationRule]s.
     *
     * Usage example:
     * ```kotlin
     * violationRules {
     *    all {
     *        minCoverageRatio.set(0.7d)
     *    }
     * }
     */
    fun all(action: Action<in ViolationRule>) {
        CoverageEntity.values().forEach { coverageEntity -> coverageEntity(action) }
    }

    /**
     * Applies the given action to the [ViolationRule] for the given [CoverageEntity].
     * This operator is designed  to be used in the following way:
     * ```kotlin
     * violationRules {
     *     CoverageEntity.LINE {
     *         minCoverageRatio.set(0.7d)
     *     }
     * }
     * ```
     */
    operator fun CoverageEntity.invoke(action: Action<in ViolationRule>) = rule(this, action)

    /**
     * Applies the given action to the [ViolationRule] for the given [CoverageEntity].
     * This function is designed to be used in the following way:
     * ```kotlin
     * violationRules {
     *     rule(CoverageEntity.LINE) {
     *         minCoverageRatio.set(0.7d)
     *     }
     * }
     * ```
     */
    fun rule(coverageEntity: CoverageEntity, action: Action<in ViolationRule>) {
        val newViolationRule: ViolationRule = rules.get().getValue(coverageEntity)
        action.execute(newViolationRule)
    }

    override fun toString(): String =
        "ViolationRules(allRules=${rules.get()}, failOnViolation=${failOnViolation.get()})"
}

open class ViolationRule @Inject constructor(
    objectFactory: ObjectFactory,
) {

    @Input
    val minCoverageRatio: Property<Double> = objectFactory.doubleProperty(0.0)

    @Input
    @Optional
    val entityCountThreshold: Property<Int?> = objectFactory.property(Int::class.java)

    override fun toString(): String {
        return "ViolationRule(minCoverageRatio=${minCoverageRatio.get()}, " +
                "entityCountThreshold=${entityCountThreshold.orNull})"
    }
}

package io.github.surpsg.deltacoverage.gradle

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

    @Optional
    @InputFiles
    var jacocoExecFiles: FileCollection? = null

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
    val violationRules: ViolationRules = objectFactory.newInstance(ViolationRules::class.java)

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
                "jacocoExecFiles=$jacocoExecFiles, " +
                "classesDirs=$classesDirs, " +
                "srcDirs=$srcDirs, " +
                "excludeClasses=${excludeClasses.get()}, " +
                "diffSource=$diffSource, " +
                "reportConfiguration=$reportConfiguration, " +
                "violationRules=$violationRules)"
    }
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
    val csv: Property<Boolean> = objectFactory.booleanProperty(false)

    @Input
    val baseReportDir: Property<String> = objectFactory.stringProperty {
        Paths.get("build", "reports", "jacoco").toString()
    }

    @Input
    val fullCoverageReport: Property<Boolean> = objectFactory.booleanProperty(false)

    override fun toString() = "ReportsConfiguration(" +
            "html=${html.get()}, " +
            "xml=${xml.get()}, " +
            "csv=${csv.get()}, " +
            "baseReportDir='$baseReportDir'"
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
            CoverageEntity.values().associateWith {
                objectFactory.newInstance(ViolationRule::class.java)
            }
        )
    }

    @Deprecated(
        message = """
        This property will be removed in the next major release.
        
        Use the following syntax instead: 
        deltaCoverageReport {
            violationRules {
                rule(io.github.surpsg.deltacoverage.gradle.CoverageEntity.LINE) {
                    minCoverageRatio.set(0.7d)
                }
            }
        }
    """,
        replaceWith = ReplaceWith("this.rule(coverageEntity, action)")
    )
    @Input
    val minLines: Property<Double> = objectFactory.doubleProperty(0.0)

    @Deprecated(
        message = """
        This property will be removed in the next major release.
        
        Use the following syntax instead: 
        deltaCoverageReport {
            violationRules {
                rule(io.github.surpsg.deltacoverage.gradle.CoverageEntity.BRANCH) {
                    minCoverageRatio.set(0.7d)
                }
            }
        }
    """,
        replaceWith = ReplaceWith("this.rule(coverageEntity, action)")
    )
    @Input
    val minBranches: Property<Double> = objectFactory.doubleProperty(0.0)

    @Deprecated(
        message = """
        This property will be removed in the next major release.
        
        Use the following syntax instead: 
        deltaCoverageReport {
            violationRules {
                rule(io.github.surpsg.deltacoverage.gradle.CoverageEntity.INSTRUCTION) {
                    minCoverageRatio.set(0.7d)
                }
            }
        }
    """,
        replaceWith = ReplaceWith("this.rule(coverageEntity, action)")
    )
    @Input
    val minInstructions: Property<Double> = objectFactory.doubleProperty(0.0)

    @Input
    val failOnViolation: Property<Boolean> = objectFactory.booleanProperty(false)

    infix fun failIfCoverageLessThan(minCoverage: Double) {
        minLines.set(minCoverage)
        minBranches.set(minCoverage)
        minInstructions.set(minCoverage)
        failOnViolation.set(true)
    }

    operator fun CoverageEntity.invoke(action: Action<in ViolationRule>) = rule(this, action)

    fun rule(coverageEntity: CoverageEntity, action: Action<in ViolationRule>) {
        val newViolationRule: ViolationRule = rules.get().getValue(coverageEntity)
        action.execute(newViolationRule)

        when (coverageEntity) {
            CoverageEntity.INSTRUCTION -> minInstructions.set(newViolationRule.minCoverageRatio.get())
            CoverageEntity.BRANCH -> minBranches.set(newViolationRule.minCoverageRatio.get())
            CoverageEntity.LINE -> minLines.set(newViolationRule.minCoverageRatio.get())
        }
    }

    override fun toString(): String {
        return "ViolationRules(" +
                "minLines=${minLines.get()}, " +
                "minBranches=${minBranches.get()}, " +
                "minInstructions=${minInstructions.get()}, " +
                "failOnViolation=${failOnViolation.get()}" +
                ")"
    }

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

private fun ObjectFactory.booleanProperty(default: Boolean): Property<Boolean> {
    return property(Boolean::class.javaObjectType).convention(default)
}

private fun ObjectFactory.doubleProperty(default: Double): Property<Double> {
    return property(Double::class.javaObjectType).convention(default)
}

private fun ObjectFactory.stringProperty(default: String): Property<String> {
    return property(String::class.javaObjectType).convention(default)
}

private fun ObjectFactory.stringProperty(default: () -> String): Property<String> {
    return property(String::class.javaObjectType).convention(
        default()
    )
}

private inline fun <reified K, reified V> ObjectFactory.map(): MapProperty<K, V> =
    mapProperty(K::class.java, V::class.java)

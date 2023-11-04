package io.github.surpsg.deltacoverage.gradle

import io.github.surpsg.deltacoverage.gradle.sources.SourceType
import io.github.surpsg.deltacoverage.gradle.sources.SourcesResolver
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class DeltaCoveragePlugin : Plugin<Project> {

    override fun apply(project: Project) {

        if (project.isAutoApplyJacocoEnabled()) {
            autoApplyJacocoPlugin(project)
        }

        val deltaCoverageConfig: DeltaCoverageConfiguration = project.extensions.create(
            DELTA_COVERAGE_REPORT_EXTENSION,
            DeltaCoverageConfiguration::class.java,
            project.objects
        )

        project.tasks.create(DELTA_COVERAGE_TASK, DeltaCoverageTask::class.java) { deltaCoverageTask ->
            with(deltaCoverageTask) {
                configureDependencies()

                projectDirProperty.set(project.projectDir)
                rootProjectDirProperty.set(project.rootProject.projectDir)
                deltaCoverageConfigProperty.set(deltaCoverageConfig)
                applySourcesInputs(deltaCoverageConfig)
            }
        }
    }

    private fun DeltaCoverageTask.configureDependencies() = project.afterEvaluate {
        val deltaCoverageTask: DeltaCoverageTask = this
        project.getAllTasks(true).values.asSequence()
            .flatMap { it.asSequence() }
            .forEach { task ->
                configureDependencyFromTask(deltaCoverageTask, task)
            }
    }

    private fun configureDependencyFromTask(deltaCoverageTask: DeltaCoverageTask, task: Task) {
        if (task.name == JavaPlugin.CLASSES_TASK_NAME) {
            log.info("Configuring {} to depend on {}", deltaCoverageTask, task)
            deltaCoverageTask.dependsOn(task)
        }

        if (task is Test) {
            log.info("Configuring {} to run after {}", deltaCoverageTask, task)
            deltaCoverageTask.mustRunAfter(task)
        }
    }

    private fun DeltaCoverageTask.applySourcesInputs(
        config: DeltaCoverageConfiguration
    ) = project.gradle.taskGraph.whenReady {
        val contextBuilder = SourcesResolver.Context.Builder.newBuilder(project, project.objects, config)
        listOf(
            classesFiles to SourceType.CLASSES,
            sourcesFiles to SourceType.SOURCES,
            coverageBinaryFiles to SourceType.COVERAGE_BINARIES,
        ).forEach { (taskSourceProperty, sourceType) ->
            taskSourceProperty.value(
                project.provider {
                    val resolveContext: SourcesResolver.Context = contextBuilder.build(sourceType)
                    SourcesResolver().resolve(resolveContext)
                }
            )
        }
    }

    private fun autoApplyJacocoPlugin(project: Project) {
        val jacocoApplied: Boolean = project.allprojects.any {
            it.pluginManager.hasPlugin(JACOCO_PLUGIN)
        }
        if (!jacocoApplied) {
            project.allprojects.forEach {
                log.info("Auto-applying $JACOCO_PLUGIN plugin to project '{}'", it.name)
                it.pluginManager.apply(JACOCO_PLUGIN)
            }
        }
    }

    private fun Project.isAutoApplyJacocoEnabled(): Boolean {
        val autoApplyValue = project.properties.getOrDefault(AUTO_APPLY_JACOCO_PROPERTY_NAME, "true")!!
        return autoApplyValue.toString().toBoolean()
    }

    companion object {
        const val AUTO_APPLY_JACOCO_PROPERTY_NAME = "io.github.surpsg.delta-coverage.auto-apply-jacoco"
        const val DELTA_COVERAGE_REPORT_EXTENSION = "deltaCoverageReport"
        const val DELTA_COVERAGE_TASK = "deltaCoverage"
        const val JACOCO_PLUGIN = "jacoco"

        val log: Logger = LoggerFactory.getLogger(DeltaCoveragePlugin::class.java)
    }

}

package io.github.surpsg.deltacoverage.gradle.sources

import io.github.surpsg.deltacoverage.CoverageEngine
import io.github.surpsg.deltacoverage.gradle.DeltaCoverageConfiguration
import io.github.surpsg.deltacoverage.gradle.sources.filter.SourceFilter
import io.github.surpsg.deltacoverage.gradle.sources.lookup.SourcesAutoLookup
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class SourcesResolver {

    fun resolve(context: Context): FileCollection {
        val sourceType: SourceType = context.sourceType
        val deltaPluginConfiguredSource: FileCollection? = getDeltaPluginConfiguredSource(context)

        val (provider, resolvedSources) = if (deltaPluginConfiguredSource == null) {
            log.debug(
                "{} is not set manually. Attempting to autoconfigure from {} coverage plugin...",
                context.sourceType,
                context.provider.pluginName
            )
            context.provider to context.sourcesAutoLookup.lookup(sourceType)
        } else {
            Provider.DELTA_COVERAGE to deltaPluginConfiguredSource
        }

        return if (resolvedSources.isEmpty) {
            throwMissedConfigurationException(provider, sourceType)
        } else {
            log.debug(
                "{}({}) files were configured from {}",
                sourceType.sourceConfigurationPath,
                sourceType.resourceName(provider),
                provider.pluginName
            )
            obtainFilterSource(context, provider, resolvedSources)
        }
    }

    private fun obtainFilterSource(
        context: Context,
        provider: Provider,
        resolvedSources: FileCollection
    ): FileCollection {
        val sourcesToFilter = SourceFilter.InputSource(resolvedSources, provider, context.sourceType)
        return SourceFilter.build(context.config, context.sourceType)
            .filter(sourcesToFilter)
    }

    private fun throwMissedConfigurationException(
        provider: Provider,
        sourceType: SourceType
    ): Nothing {
        val errorMessage = if (provider == Provider.DELTA_COVERAGE) {
            "'${sourceType.sourceConfigurationPath}' file collection is empty."
        } else {
            "'${sourceType.sourceConfigurationPath}' is not configured."
        }
        error(errorMessage)
    }

    private fun getDeltaPluginConfiguredSource(
        context: Context,
    ): FileCollection? {
        return when (context.sourceType) {
            SourceType.SOURCES -> null
            SourceType.CLASSES -> context.config.classesDirs
            SourceType.COVERAGE_BINARIES -> context.config.reportViews.getAt(context.viewName).coverageBinaryFiles
        }
    }

    class Context private constructor(
        val viewName: String,
        val sourcesAutoLookup: SourcesAutoLookup,
        val sourceType: SourceType,
        private val builder: Builder,
    ) {

        val config: DeltaCoverageConfiguration
            get() = builder.config

        val provider: Provider
            get() = Provider.fromEngine(builder.config.coverage.engine.get())

        class Builder private constructor(
            private val objectFactory: ObjectFactory,
            val project: Project,
            val config: DeltaCoverageConfiguration,
        ) {

            fun build(
                viewName: String,
                sourceType: SourceType
            ): Context {
                val sourceAutoLookup = SourcesAutoLookup.build(
                    config.coverage.engine.get(),
                    SourcesAutoLookup.Context(project, config, objectFactory)
                )
                return Context(viewName, sourceAutoLookup, sourceType, this)
            }

            companion object {

                fun newBuilder(
                    project: Project,
                    objectFactory: ObjectFactory,
                    config: DeltaCoverageConfiguration
                ): Builder = Builder(
                    objectFactory,
                    project,
                    config,
                )
            }
        }
    }

    enum class Provider(val pluginName: String) {
        JACOCO("JaCoCo"),
        KOVER("Kover"),
        DELTA_COVERAGE("Delta-Coverage"),
        ;

        companion object {

            fun fromEngine(engine: CoverageEngine): Provider = when (engine) {
                CoverageEngine.JACOCO -> JACOCO
                CoverageEngine.INTELLIJ -> KOVER
            }
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(SourcesResolver::class.java)
    }
}

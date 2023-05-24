package io.github.surpsg.deltacoverage.report

import io.github.surpsg.deltacoverage.config.DeltaCoverageConfig
import io.github.surpsg.deltacoverage.diff.CodeUpdateInfo
import io.github.surpsg.deltacoverage.diff.DiffSource
import io.github.surpsg.deltacoverage.diff.parse.ModifiedLinesDiffParser
import java.io.File

class ReportContext(
    val diffSource: DiffSource,
    val deltaCoverageConfig: DeltaCoverageConfig
) {
    internal val binaryCoverageFiles: Set<File> = deltaCoverageConfig.coverageBinaryFiles.filter(File::exists).toSet()
    internal val classFiles: Set<File> = deltaCoverageConfig.classFiles.filter(File::exists).toSet()
    internal val srcFiles: Set<File> = deltaCoverageConfig.sourceFiles.filter(File::exists).toSet()

    internal val codeUpdateInfo: CodeUpdateInfo by lazy {
        val changesMap = ModifiedLinesDiffParser().collectModifiedLines(
            diffSource.pullDiff()
        )
        CodeUpdateInfo(changesMap)
    }
}

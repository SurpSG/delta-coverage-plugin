package io.github.surpsg.deltacoverage.incubator

import com.intellij.rt.coverage.data.ClassData
import com.intellij.rt.coverage.data.FileMapData
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.instrumentation.UnloadedUtil
import com.intellij.rt.coverage.instrumentation.offline.RawReportLoader
import com.intellij.rt.coverage.report.data.BinaryReport
import com.intellij.rt.coverage.report.data.Module
import com.intellij.rt.coverage.util.ProjectDataLoader
import com.intellij.rt.coverage.util.classFinder.ClassFilter
import com.intellij.rt.coverage.util.classFinder.ClassFinder
import com.intellij.rt.coverage.util.classFinder.ClassPathEntry
import io.github.surpsg.deltacoverage.diff.ClassModifications
import io.github.surpsg.deltacoverage.diff.CodeUpdateInfo
import io.github.surpsg.deltacoverage.diff.parse.ClassFile
import java.io.IOException

fun getProjectData(
    binaryCoverageReports: List<BinaryReport>,
    sources: List<Module>,
    codeUpdateInfo: CodeUpdateInfo
): ProjectData {
    val hasRawHitsReport: Boolean = binaryCoverageReports.any { it.isRawHitsReport }

    // Note that instructions collection is done only inside this method
    // to ensure that instructions count in inline methods
    // correspond to method definition, not method call
    val projectData: ProjectData = collectCoverageInformationFromOutputs(sources)
    val projectDataCopy = if (hasRawHitsReport) {
        projectData
    } else {
        null
    }
    for (report in binaryCoverageReports) {
        if (report.isRawHitsReport) {
            try {
                RawReportLoader.load(report.dataFile, projectDataCopy)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        } else {
            val data = ProjectDataLoader.load(report.dataFile)
            mergeHits(projectData, data, codeUpdateInfo)
        }
    }
    if (projectDataCopy != null) {
        projectDataCopy.applyLineMappings()
        mergeHits(projectData, projectDataCopy, codeUpdateInfo)
    }
    return copyProjectDataWithFiltering(projectData, codeUpdateInfo)
}

private fun collectCoverageInformationFromOutputs(modules: List<Module>): ProjectData {
    val projectData = ProjectData().apply {
        setInstructionsCoverage(true)
        annotationsToIgnore = emptyList()
    }
    UnloadedUtil.appendUnloaded(
        projectData,
        OutputClassFinder(modules),
        true,
        true
    )
    return projectData
}

private fun mergeHits(dst: ProjectData, src: ProjectData, codeUpdateInfo: CodeUpdateInfo) {
    for (srcClass in src.classesCollection) {

        val dstClass: ClassData = dst.getClassData(srcClass.name) ?: continue

        val classFile = classFileFrom(dstClass)
        if (!codeUpdateInfo.isInfoExists(classFile)) {
            continue
        }

        dstClass.merge(srcClass)

        val classModifications: ClassModifications = codeUpdateInfo.getClassModifications(classFile)
        dstClass.filterLines(classModifications)
    }
}

private fun ClassData.filterLines(classModifications: ClassModifications) {
    val lines: Array<LineData?> = classLines()
    for (i in lines.indices) {
        val actualLine: LineData = lines[i] ?: continue
        if (!classModifications.isLineModified(actualLine.lineNumber)) {
            lines[i] = null
        }
    }
}

private fun ClassData.classLines(): Array<LineData?> {
    return lines as? Array<LineData?>? ?: emptyArray()
}

private fun copyProjectDataWithFiltering(projectData: ProjectData, codeUpdateInfo: CodeUpdateInfo): ProjectData {
    val projectDataCopy = ProjectData.createProjectData(true)
    for (classData in projectData.classesCollection) {
        val classFile: ClassFile = classFileFrom(classData)

        if (!codeUpdateInfo.isInfoExists(classFile)) {
            continue
        }

        projectDataCopy.getOrCreateClassData(classData.name).apply {
            source = classData.source
            merge(classData)
        }
    }
    val mappings: Map<String, Array<FileMapData>> = projectData.linesMap ?: emptyMap()

    mappings.forEach { (key, value) ->
        projectDataCopy.addLineMaps(key, value)
    }
    return projectDataCopy
}

private fun classFileFrom(classData: ClassData) = ClassFile(
    sourceFileName = classData.source,
    className = classData.name
)

internal class OutputClassFinder(
    private val modules: List<Module>
) : ClassFinder(filter) {

    override fun getClassPathEntries(): Collection<ClassPathEntry> {
        val entries: MutableList<ClassPathEntry> = ArrayList()
        for (module in modules) {
            val outputs = module.outputRoots ?: continue
            for (outputRoot in outputs) {
                entries.add(ClassPathEntry(outputRoot.absolutePath))
            }
        }
        return entries
    }
}

val filter: ClassFilter = ClassFilter { true }

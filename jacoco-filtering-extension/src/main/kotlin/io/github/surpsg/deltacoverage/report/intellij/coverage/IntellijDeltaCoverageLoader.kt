package io.github.surpsg.deltacoverage.report.intellij.coverage

import com.intellij.rt.coverage.data.ClassData
import com.intellij.rt.coverage.data.FileMapData
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.data.instructions.ClassInstructions
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

internal object IntellijDeltaCoverageLoader {

    fun getDeltaProjectData(
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
                val data = ProjectDataLoader.load(report.dataFile).apply {
                    setInstructionsCoverage(true)
                }
                mergeHitsWithFiltering(projectData, data, codeUpdateInfo)
            }
        }
        if (projectDataCopy != null) {
            projectDataCopy.applyLineMappings()
            mergeHitsWithFiltering(projectData, projectDataCopy, codeUpdateInfo)
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
        return projectData.apply {
            setInstructionsCoverage(true)
        }
    }

    private fun mergeHitsWithFiltering(dst: ProjectData, src: ProjectData, codeUpdateInfo: CodeUpdateInfo) {
        for (srcClass in src.classesCollection) {

            val dstClass: ClassData = dst.getClassData(srcClass.name) ?: continue

            val classFile = classFileFrom(dstClass)
            if (codeUpdateInfo.isInfoExists(classFile)) {
                dstClass.merge(srcClass)

                val classModifications: ClassModifications = codeUpdateInfo.getClassModifications(classFile)
                dstClass.filterLines(classModifications)
            }
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

    private fun copyProjectDataWithFiltering(
        sourceProjectData: ProjectData,
        codeUpdateInfo: CodeUpdateInfo
    ): ProjectData {
        val projectDataCopy = ProjectData.createProjectData(true).apply {
            setInstructionsCoverage(true)
        }
        sourceProjectData.copyAllClassDataWithFiltering(projectDataCopy, codeUpdateInfo)

        val mappings: Map<String, Array<FileMapData>> = sourceProjectData.linesMap ?: emptyMap()
        mappings.forEach { (key, value) ->
            projectDataCopy.addLineMaps(key, value)
        }
        return projectDataCopy
    }

    private fun ProjectData.copyAllClassDataWithFiltering(
        copyToProjectData: ProjectData,
        codeUpdateInfo: CodeUpdateInfo,
    ) {
        val sourceProjectData: ProjectData = this
        sourceProjectData.classesCollection.asSequence()
            .filter { sourceClassData ->
                val classFile: ClassFile = classFileFrom(sourceClassData)
                codeUpdateInfo.isInfoExists(classFile)
            }
            .map { sourceClassData ->
                ClassDataCopingContext(sourceClassData.name, sourceProjectData, copyToProjectData)
            }
            .forEach { classCopyContext ->
                classCopyContext.copyClassData()
            }
    }

    private data class ClassDataCopingContext(
        val className: String,
        val sourceProjectData: ProjectData,
        val copyToProjectData: ProjectData,
    ) {

        val sourceClassData: ClassData
            get() = sourceProjectData.getOrCreateClassData(className)

        private val allSourceClassInstructions: Map<String, ClassInstructions>
            get() = sourceProjectData.instructions

        private val copyToClassData: ClassData
            get() = copyToProjectData.getOrCreateClassData(className)

        private val copyToClassInstructions: ClassInstructions
            get() = copyToProjectData.instructions.computeIfAbsent(className) {
                ClassInstructions()
            }

        fun copyClassData() {
            val sourceClass: ClassData = sourceClassData
            copyToClassData.apply {
                source = sourceClass.source
                merge(sourceClass)
            }

            copyClassInstructions()
        }

        private fun copyClassInstructions() {
            val className: String = className
            allSourceClassInstructions[className]?.let { sourceClassInstructions ->
                copyToClassInstructions.merge(sourceClassInstructions, copyToClassData)
            }
        }
    }

    private fun classFileFrom(classData: ClassData) = ClassFile(
        sourceFileName = classData.source,
        className = classData.name
    )

    private class OutputClassFinder(
        private val modules: List<Module>
    ) : ClassFinder(IncludeAllClassFilter) {

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

        private object IncludeAllClassFilter: ClassFilter {
            override fun shouldInclude(className: String?): Boolean = true
        }
    }

}

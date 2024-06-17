package io.github.surpsg.deltacoverage.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class NativeGitDiffTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {

    @get:Input
    val targetBranch: Property<String> = objectFactory.property(String::class.java)

    @get:OutputFile
    val diffFile: RegularFileProperty = objectFactory.fileProperty()
        .convention(project.layout.buildDirectory.file("code-diff.diff"))

    init {
        description = "Generate a diff file using native git."
    }

    @TaskAction
    fun obtainDiff() {
        val file: File = diffFile.get().asFile
        file.outputStream().use { fileOutputStream ->
            project.exec {
                it.setCommandLine("git", "diff", "--no-color", "--minimal", "${targetBranch.get()}...")
                it.standardOutput = fileOutputStream
                it.errorOutput = System.err
            }

            println("Diff file generated: file://${file.absolutePath}")
        }
    }
}

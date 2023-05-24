package io.github.surpsg.deltacoverage.report

import java.io.File

abstract class DeltaReportGeneratorFacade(
    protected val reportContext: ReportContext
) {

    abstract fun generateReport(): DeltaReportGeneratorFacade

    fun saveDiffTo(file: File, onSuccess: (File) -> Unit = {}): DeltaReportGeneratorFacade {
        val saveDiffTo: File = reportContext.diffSource.saveDiffTo(file)
        onSuccess(saveDiffTo)

        return this
    }

}

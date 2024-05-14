package io.github.surpsg.deltacoverage.report

abstract class DeltaReportGeneratorFacade(
    protected val reportContext: ReportContext
) {

    abstract fun generateReport(): DeltaReportGeneratorFacade
}

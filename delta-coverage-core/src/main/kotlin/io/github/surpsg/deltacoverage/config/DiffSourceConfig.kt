package io.github.surpsg.deltacoverage.config

class DiffSourceConfig private constructor(
    val file: String = "",
    val url: String = "",
    val diffBase: String = ""
) {

    init {
        val initializedCount = sequenceOf(file, url, diffBase).filter { it.isNotBlank() }.count()
        require(initializedCount == 1) {
            "Required single diff source initialized but was: $this"
        }
    }

    override fun toString(): String {
        return "DiffSourceConfig(file='$file', url='$url', diffBase='$diffBase')"
    }

    @DeltaCoverageConfigMarker
    class Builder internal constructor() {
        var file: String = ""
        var url: String = ""
        var diffBase: String = ""

        internal fun build(): DiffSourceConfig = DiffSourceConfig(file, url, diffBase)
    }

    companion object {

        operator fun invoke(customize: Builder.() -> Unit): DiffSourceConfig =
            Builder().apply(customize).build()
    }
}

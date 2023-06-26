package io.github.surpsg.deltacoverage.config

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeBlank
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DiffSourceConfigTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            " ' ' | '  ' | ' ' ",
            " ' ' | file | git ",
            " url | file | ' ' ",
            " url | ' '  | git ",
            " url | file | git ",
        ]
    )
    fun `should throw if specified more than one diff source or zero`(
        urlValue: String,
        fileValue: String,
        gitValue: String
    ) {
        shouldThrow<IllegalArgumentException> {
            DiffSourceConfig {
                file = fileValue
                diffBase = gitValue
                url = urlValue
            }
        }
    }

    @Test
    fun `should build diff source with git if specified only git`() {
        // GIVEN
        val sourceValue = "git"

        // WHEN
        val actual = DiffSourceConfig { diffBase = sourceValue }

        // THEN
        assertSoftly(actual) {
            diffBase shouldBe sourceValue
            file.shouldBeBlank()
            url.shouldBeBlank()
        }
    }

    @Test
    fun `should build file source with git if specified only file`() {
        // GIVEN
        val sourceValue = "file"

        // WHEN
        val actual = DiffSourceConfig { file = sourceValue }

        // THEN
        assertSoftly(actual) {
            file shouldBe sourceValue
            diffBase.shouldBeBlank()
            url.shouldBeBlank()
        }
    }

    @Test
    fun `should build url source with git if specified only url`() {
        // GIVEN
        val sourceValue = "url"

        // WHEN
        val actual = DiffSourceConfig { url = sourceValue }

        // THEN
        assertSoftly(actual) {
            url shouldBe sourceValue
            diffBase.shouldBeBlank()
            file.shouldBeBlank()
        }
    }
}

package io.github.surpsg.deltacoverage.diff

import io.github.surpsg.deltacoverage.config.DiffSourceConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.startWith
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.mockk
import java.io.File

class DiffSourceFactoryTest : StringSpec() {
    init {

        "buildDiffSource should return file diff source" {
            // setup
            val filePath = "someFile"
            val diffConfig = DiffSourceConfig { file = filePath }

            // run
            val diffSource = DiffSource.buildDiffSource(File("."), diffConfig)

            // assert
            diffSource.shouldBeTypeOf<FileDiffSource>()
            diffSource.sourceDescription shouldBe "File: $filePath"
        }

        "buildDiffSource should return url diff source" {
            // setup
            val expectedUrl = "someUrl"
            val diffConfig = DiffSourceConfig { url = expectedUrl }

            // run
            val diffSource = DiffSource.buildDiffSource(File("."), diffConfig)

            // assert
            diffSource.shouldBeTypeOf<UrlDiffSource>()
            diffSource.sourceDescription shouldBe "URL: $expectedUrl"
        }

        "buildDiffSource should return git diff source" {
            // setup
            val compareWith = "develop"
            val diffConfig = DiffSourceConfig { diffBase = compareWith }

            // run
            val diffSource = DiffSource.buildDiffSource(File("."), diffConfig)

            // assert
            diffSource.shouldBeTypeOf<GitDiffSource>()
            diffSource.sourceDescription shouldBe "Git: diff $compareWith"
        }

        "buildDiffSource should throw when no source specified" {
            // setup
            val diffConfig = mockk<DiffSourceConfig> {
                every { file } returns ""
                every { url } returns ""
                every { diffBase } returns ""
            }
            // run
            val exception = shouldThrow<IllegalStateException> {
                DiffSource.buildDiffSource(File("."), diffConfig)
            }

            // assert
            exception.message should startWith(
                "Expected Git configuration or file or URL diff source but all are blank"
            )
        }
    }
}

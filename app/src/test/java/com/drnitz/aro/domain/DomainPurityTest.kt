package com.drnitz.aro.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Architectural guard: the domain layer must stay pure Kotlin.
 *
 * Task 4's whole value proposition is that these formulas run as fast JVM unit tests and
 * could later be lifted into a shared module. A single stray `import androidx.compose...`
 * would pull the Android toolchain back in and break that, so the rule is enforced by a
 * test rather than a code-review convention.
 */
class DomainPurityTest {

    private fun locateDomainDir(): File {
        val candidates = listOf(
            "src/main/java/com/drnitz/aro/domain",                              // module dir
            "app/src/main/java/com/drnitz/aro/domain",                          // android/ dir
            "android/app/src/main/java/com/drnitz/aro/domain",                  // repo root
        )
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            for (candidate in candidates) {
                val resolved = File(dir, candidate)
                if (resolved.isDirectory) return resolved
            }
            dir = dir.parentFile
        }
        throw AssertionError(
            "Could not locate the domain source directory from " +
                System.getProperty("user.dir") + " — the purity guard cannot verify anything",
        )
    }

    private fun domainSources(): List<File> =
        locateDomainDir().walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun `domain sources are discoverable`() {
        val sources = domainSources()
        assertTrue("Expected domain sources to scan", sources.size >= 6)
    }

    @Test
    fun `no domain file imports android or androidx`() {
        val offenders = domainSources().flatMap { file ->
            file.readLines()
                .filter { line -> line.trimStart().startsWith("import android") }
                .map { line -> "${file.name}: $line" }
        }
        assertTrue(
            "The domain layer must stay pure Kotlin, but found:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `no domain file references compose types`() {
        val offenders = domainSources().flatMap { file ->
            file.readLines()
                .filter { line -> line.contains("androidx.compose") || line.contains("@Composable") }
                .map { line -> "${file.name}: $line" }
        }
        assertTrue(offenders.joinToString("\n"), offenders.isEmpty())
    }
}
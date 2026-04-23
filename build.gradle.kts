// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dagger.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
    alias(libs.plugins.google.firebase.perf) apply false
}

tasks.register("ciTest") {
    dependsOn(":app:testDebugUnitTest", ":app:connectedDebugAndroidTest")
}

tasks.register("verifyWarningBudget") {
    group = "verification"
    description = "Fails build when warning counts exceed the agreed baseline budget."

    doLast {
        val lintReport = file("app/build/reports/lint-results-debug.xml")
        if (!lintReport.exists()) {
            throw GradleException(
                "Missing lint report at ${lintReport.path}. Run :app:lintDebug before verifyWarningBudget.",
            )
        }

        val baselineByCategory = linkedMapOf(
            "deprecation" to 24,
            "nullability" to 16,
            "api-migration" to 18,
            "tooling" to 12,
        )

        val categoryByIssueId = mapOf(
            "Deprecated" to "deprecation",
            "NewApi" to "api-migration",
            "InlinedApi" to "api-migration",
            "UnknownNullness" to "nullability",
            "NullSafeMutableLiveData" to "nullability",
            "SyntheticAccessor" to "tooling",
            "GradleDependency" to "tooling",
            "ObsoleteLintCustomCheck" to "tooling",
            "ObsoleteSdkInt" to "tooling",
        )

        val warningCounts = baselineByCategory.keys.associateWith { 0 }.toMutableMap()
        val uncategorizedWarnings = mutableListOf<String>()

        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            setExpandEntityReferences(false)
        }
        val documentBuilder = documentBuilderFactory.newDocumentBuilder()
        val document = documentBuilder.parse(lintReport)
        val issueNodes = document.getElementsByTagName("issue")

        for (index in 0 until issueNodes.length) {
            val issue = issueNodes.item(index)
            val severity = issue.attributes?.getNamedItem("severity")?.nodeValue
            if (severity != "Warning") continue

            val issueId = issue.attributes?.getNamedItem("id")?.nodeValue ?: continue
            val category = categoryByIssueId[issueId]
            if (category == null) {
                uncategorizedWarnings += issueId
                continue
            }
            warningCounts[category] = warningCounts.getValue(category) + 1
        }

        val reportDir = file("app/build/reports/warnings")
        reportDir.mkdirs()
        val summaryFile = File(reportDir, "warning-budget-summary.md")
        summaryFile.writeText(
            buildString {
                appendLine("# Warning Budget Report")
                appendLine()
                appendLine("| Category | Baseline | Current | Delta |")
                appendLine("|---|---:|---:|---:|")
                baselineByCategory.forEach { (category, baseline) ->
                    val current = warningCounts.getValue(category)
                    appendLine("| $category | $baseline | $current | ${current - baseline} |")
                }
                appendLine()
                if (uncategorizedWarnings.isNotEmpty()) {
                    appendLine("## Uncategorized warning IDs")
                    uncategorizedWarnings.distinct().sorted().forEach { appendLine("- $it") }
                }
            },
        )

        val regressions = baselineByCategory
            .mapNotNull { (category, baseline) ->
                val current = warningCounts.getValue(category)
                if (current > baseline) "$category: $current > baseline $baseline" else null
            } + if (uncategorizedWarnings.isNotEmpty()) {
            "uncategorized: ${uncategorizedWarnings.distinct().size} > baseline 0"
        } else {
            emptyList<String>()
        }

        if (regressions.isNotEmpty()) {
            throw GradleException(
                "Warning budget exceeded: ${regressions.joinToString("; ")}. " +
                    "See ${summaryFile.path} for details.",
            )
        }
    }
}

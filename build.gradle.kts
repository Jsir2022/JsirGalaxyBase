
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import java.security.MessageDigest
import java.net.URI
import java.util.zip.ZipFile

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

evaluationDependsOn(":ui2-core")
evaluationDependsOn(":ui2-terminal")
evaluationDependsOn(":ui2-lab")
evaluationDependsOn(":galaxy-quest-core")
evaluationDependsOn(":galaxy-quest-postgres")
val ui2CoreJar = project(":ui2-core").tasks.named<Jar>("jar")
val ui2TerminalJar = project(":ui2-terminal").tasks.named<Jar>("jar")
val ui2LabJar = project(":ui2-lab").tasks.named<Jar>("jar")
val galaxyQuestCoreJar = project(":galaxy-quest-core").tasks.named<Jar>("jar")
val galaxyQuestPostgresJar = project(":galaxy-quest-postgres").tasks.named<Jar>("jar")
val ui2GeneratedResources = layout.buildDirectory.dir("generated/ui2-resources")
val ui2QzLicenseUrl = "https://raw.githubusercontent.com/QuanhuZeYu/Qz-UILib/7937cd042910c8182d899d41aa5692d1c8b4a98a/LICENSE"
val ui2QzLicenseSha256 = "f7b20465c7ba7e58284504c6eb2acc16287a0540a06c3a1b5d31df10daa29580"

fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it) }

val prepareUi2QzLicense = tasks.register("prepareUi2QzLicense") {
    val license = ui2GeneratedResources.map { it.file("META-INF/licenses/Qz-UILib-LGPL-3.0-or-later.txt") }
    inputs.property("licenseUrl", ui2QzLicenseUrl)
    inputs.property("licenseSha256", ui2QzLicenseSha256)
    outputs.file(license)
    doLast {
        val bytes = URI(ui2QzLicenseUrl).toURL().openStream().use { it.readBytes() }
        val actual = sha256(bytes)
        check(actual == ui2QzLicenseSha256) {
            "Checksum mismatch for $ui2QzLicenseUrl: expected $ui2QzLicenseSha256, got $actual"
        }
        license.get().asFile.apply { parentFile.mkdirs(); writeBytes(bytes) }
    }
}

dependencies {
    implementation(project(":ui2-core"))
    implementation(project(":ui2-terminal"))
    implementation(project(":ui2-lab"))
    implementation(project(":galaxy-quest-core"))
    implementation(project(":galaxy-quest-postgres"))
    shadowImplementation(files(ui2CoreJar))
    shadowImplementation(files(ui2TerminalJar))
    shadowImplementation(files(ui2LabJar))
    // Phase 0 compiles against the GTNH-pinned AE2 API but never bundles AE2.
    compileOnly("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-695-GTNH:dev") {
        isTransitive = false
    }
}

tasks.named<Jar>("shadowJar") {
    dependsOn(ui2CoreJar, ui2TerminalJar, ui2LabJar, galaxyQuestCoreJar, galaxyQuestPostgresJar)
    from(galaxyQuestCoreJar.map { zipTree(it.archiveFile.get().asFile) })
    from(galaxyQuestPostgresJar.map { zipTree(it.archiveFile.get().asFile) })
}

val testSourceSet = the<SourceSetContainer>()["test"]

tasks.named<ProcessResources>("processResources") {
    dependsOn(prepareUi2QzLicense)
    from(ui2GeneratedResources)
    from(rootProject.file("Reference/ServerUtilities/LICENSE.txt")) {
        into("META-INF/licenses")
        rename("LICENSE.txt", "ServerUtilities-LGPL-3.0-or-later.txt")
    }
    from(rootProject.file("Reference/Applied-Energistics-2-Unofficial/LICENSE.txt")) {
        into("META-INF/licenses")
        rename("LICENSE.txt", "Applied-Energistics-2-LGPL-3.0-or-later.txt")
    }
    from(rootProject.file("THIRD_PARTY_NOTICES.md")) {
        into("META-INF")
    }
    from(rootProject.file("licenses/Qz-UILib/LICENSE-1.8.2-MIT.txt")) {
        into("META-INF/licenses")
        rename("LICENSE-1.8.2-MIT.txt", "Qz-UILib-1.8.2-MIT.txt")
    }
    from(rootProject.file("licenses/Qz-UILib/PROVENANCE.md")) {
        into("META-INF")
        rename("PROVENANCE.md", "qz-uilib-provenance.md")
    }
    from(rootProject.file("licenses/BetterQuesting/LICENSE-MIT.txt")) {
        into("META-INF/licenses")
        rename("LICENSE-MIT.txt", "BetterQuesting-MIT.txt")
    }
    from(rootProject.file("licenses/BetterQuesting/PROVENANCE.md")) {
        into("META-INF")
        rename("PROVENANCE.md", "betterquesting-provenance.md")
    }
}

val verifyUi2SelfContained = tasks.register("verifyUi2SelfContained") {
    group = "verification"
    description = "Verifies UI2 is self-contained, packages provenance, and cannot regress to legacy Canvas."
    dependsOn(tasks.named("shadowJar"))
    inputs.files(fileTree("src/main/java") { include("**/*.java") })
    inputs.files(fileTree("ui2-terminal/src/main/java") { include("**/*.java") })
    inputs.files(tasks.named("shadowJar").map { it.outputs.files })
    doLast {
        val forbiddenSource = listOf(
            "club" + ".heiqi",
            "qz_" + "uilib",
            "com.jsirgalaxybase.client.gui.framework",
            "com.jsirgalaxybase.client.gui.theme"
        )
        fileTree("src/main/java") { include("**/*.java") }.files.forEach { source ->
            val value = source.readText()
            forbiddenSource.forEach { token ->
                check(!value.contains(token, ignoreCase = true)) {
                    "UI2 production source references forbidden Qz runtime token '$token': $source"
                }
            }
        }
        val platformTokens = listOf("net.minecraft", "cpw.mods", "org.lwjgl", "appeng.")
        fileTree("ui2-terminal/src/main/java") { include("**/*.java") }.files.forEach { source ->
            val value = source.readText()
            platformTokens.forEach { token ->
                check(!value.contains(token)) {
                    "Pure ui2-terminal source references platform token '$token': $source"
                }
            }
        }
        val jar = tasks.named("shadowJar").get().outputs.files.singleFile
        ZipFile(jar).use { zip ->
            val entries = zip.entries().asSequence().toList()
            check(entries.none { it.name.startsWith("com/jsirgalaxybase/client/gui/framework/") }) {
                "Legacy Canvas framework was bundled in $jar"
            }
            check(entries.none { it.name.startsWith("com/jsirgalaxybase/client/gui/theme/") }) {
                "Legacy terminal theme implementation was bundled in $jar"
            }
            check(entries.none { it.name.startsWith("club/heiqi/") }) { "Qz classes were bundled in $jar" }
            check(entries.any { it.name == "com/jsirgalaxybase/ui2/terminal/TerminalVisualScenario.class" }) {
                "ui2-terminal classes were not embedded in $jar"
            }
            check(entries.any { it.name == "com/jsirgalaxybase/quest/core/AuthenticatedQuestClaimService.class" }) {
                "galaxy-quest-core classes were not embedded in $jar"
            }
            check(entries.any { it.name == "com/jsirgalaxybase/quest/postgres/JdbcQuestCenterQuery.class" }) {
                "galaxy-quest-postgres classes were not embedded in $jar"
            }
            check(entries.none { it.name.contains("NotoSansCJK", ignoreCase = true)
                || it.name.contains("NotoSansCJK-OFL", ignoreCase = true) }) {
                "Removed bundled CJK font was still packaged in $jar"
            }
            check(entries.none { it.name.contains("qz", ignoreCase = true) && it.name.endsWith(".mixins.json") }) {
                "Qz Mixin configuration was bundled in $jar"
            }
            entries.filter { it.name.endsWith(".class") }.forEach { entry ->
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                val value = bytes.toString(Charsets.ISO_8859_1)
                check(!value.contains("club/heiqi") && !value.contains("qz_uilib")) {
                    "Qz runtime reference found in class ${entry.name}"
                }
            }
            listOf(
                "META-INF/THIRD_PARTY_NOTICES.md",
                "META-INF/qz-uilib-provenance.md",
                "META-INF/licenses/Qz-UILib-1.8.2-MIT.txt",
                "META-INF/licenses/Qz-UILib-LGPL-3.0-or-later.txt"
            ).forEach { required -> check(zip.getEntry(required) != null) { "Missing $required from $jar" } }
        }
    }
}

tasks.named("check") { dependsOn(verifyUi2SelfContained) }

tasks.register<Test>("bankingIt") {
    group = "verification"
    description = "Runs PostgreSQL-backed banking integration tests for the banking module."
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    useJUnit()
    include("**/BankingPostgresIntegrationTest.class")
    shouldRunAfter(tasks.named("test"))
}

tasks.register("banking-it") {
    group = "verification"
    description = "Alias for bankingIt."
    dependsOn("bankingIt")
}

tasks.register<Test>("marketIt") {
    group = "verification"
    description = "Runs PostgreSQL-backed market integration tests for the market module."
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    useJUnit()
    include("**/MarketPostgresIntegrationTest.class")
    shouldRunAfter(tasks.named("test"))
}

tasks.register("market-it") {
    group = "verification"
    description = "Alias for marketIt."
    dependsOn("marketIt")
}

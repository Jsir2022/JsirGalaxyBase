
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

dependencies {
    // Phase 0 compiles against the GTNH-pinned AE2 API but never bundles AE2.
    compileOnly("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-695-GTNH:dev") {
        isTransitive = false
    }
}

val testSourceSet = the<SourceSetContainer>()["test"]

tasks.named<ProcessResources>("processResources") {
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
}

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

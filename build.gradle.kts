
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val testSourceSet = the<SourceSetContainer>()["test"]

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

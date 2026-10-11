plugins {
    application
}

group = "com.jsirgalaxybase"
version = rootProject.version

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}

dependencies {
    implementation(project(":galaxy-quest-core"))
    implementation(project(":galaxy-quest-postgres"))
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.postgresql:postgresql:42.7.4")
}

application {
    mainClass.set("com.jsirgalaxybase.quest.bqimport.BqMigrationMain")
}

tasks.register<JavaExec>("behaviorInventory") {
    group = "verification"
    description = "Reads only a BetterQuesting DefaultQuests definition directory and reports behavior usage"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.jsirgalaxybase.quest.bqimport.BqInventoryMain")
    doFirst {
        val source = providers.gradleProperty("bqDefaultQuests").orNull
            ?: throw GradleException("Pass -PbqDefaultQuests=<DefaultQuests directory>")
        args(source)
    }
}

tasks.register<JavaExec>("progressInventory") {
    group = "verification"
    description = "Reads BetterQuesting player progress without modifying it"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.jsirgalaxybase.quest.bqimport.BqProgressInventoryMain")
}

tasks.register<JavaExec>("progressConflictPreview") {
    group = "verification"
    description = "Compares two BetterQuesting progress directories without modifying either source"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.jsirgalaxybase.quest.bqimport.BqProgressConflictPreviewMain")
}

tasks.register<JavaExec>("progressMigrationPreview") {
    group = "verification"
    description = "Produces a private, read-only multi-source progress migration report (pass --args)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.jsirgalaxybase.quest.bqimport.BqProgressMigrationPreviewMain")
}

tasks.test {
    useJUnit()
}

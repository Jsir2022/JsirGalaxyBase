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
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("com.jsirgalaxybase.quest.bqimport.BqInventoryMain")
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

tasks.test {
    useJUnit()
}

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
    implementation(project(":ui2-terminal"))
    implementation(project(":ui2-lab"))
    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("com.jsirgalaxybase.ui2.demo.UiLabApplication")
}

tasks.test {
    useJUnit()
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("renderGolden") {
    group = "verification"
    description = "Renders headless Galaxy UI 2 demo PNG and DrawList snapshots."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.jsirgalaxybase.ui2.demo.UiLabExporter")
    args(layout.buildDirectory.dir("ui-lab").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("verifyTerminalVisuals") {
    group = "verification"
    description = "Renders and structurally verifies production terminal UI2 scenarios."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.jsirgalaxybase.ui2.demo.TerminalVisualVerifier")
    args(layout.buildDirectory.dir("ui-lab").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}

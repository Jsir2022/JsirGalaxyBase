plugins {
    `java-library`
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
    api(project(":ui2-core"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

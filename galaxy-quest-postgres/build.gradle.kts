plugins {
    `java-library`
}

group = "com.jsirgalaxybase"
version = rootProject.version

repositories { mavenCentral() }

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}

dependencies {
    api(project(":galaxy-quest-core"))
    implementation("com.google.code.gson:gson:2.10.1")
    runtimeOnly("org.postgresql:postgresql:42.7.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.postgresql:postgresql:42.7.4")
    testImplementation("org.mockito:mockito-core:4.11.0")
}

tasks.test { useJUnit() }

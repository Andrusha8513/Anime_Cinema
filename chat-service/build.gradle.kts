plugins {
    java
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-arc")

    // ============ REST ============
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // ============ WEBSOCKET  ============
    implementation("io.quarkus:quarkus-websockets-next")

    // ============ SCYLLADB / CASSANDRA ============
    implementation("com.datastax.oss:java-driver-core:4.17.0")
    implementation("com.datastax.oss:java-driver-query-builder:4.17.0")


    // ============ JWT  ============
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // ============ KAFKA  ============
    implementation("io.quarkus:quarkus-messaging-kafka")

    // ============ ВАЛИДАЦИЯ ============
    implementation("io.quarkus:quarkus-hibernate-validator")

    // ============ КОНФИГ ============
    implementation("io.quarkus:quarkus-config-yaml")

    // ============ MONITORING ============
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-logging-json")

    // ============ LOMBOK  ============
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    // ============ ТЕСТЫ ============
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

group = "ara"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") {
    isEnabled = false
}

tasks.named<Jar>("jar") {
    isEnabled = true
}

dependencies {
    // jpa
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // querydsl
    api("com.querydsl:querydsl-core:5.1.0")
    api("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")

    // log
    api("io.github.microutils:kotlin-logging:3.0.5")
}


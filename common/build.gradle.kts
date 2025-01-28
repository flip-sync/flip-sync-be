import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") {
    isEnabled = false
}

tasks.named<Jar>("jar") {
    isEnabled = true
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    // jpa
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    //validation
    api("org.springframework.boot:spring-boot-starter-validation")

    // swagger
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3")


    // querydsl
    api("com.querydsl:querydsl-core:5.1.0")
    api("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")

    // log
    api("io.github.microutils:kotlin-logging:3.0.5")
}


import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") {
    isEnabled = true
}

tasks.named<Jar>("jar") {
    isEnabled = true
}

dependencies {
    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // security
    api("org.springframework.boot:spring-boot-starter-security")

    // mailer
    api("org.springframework.boot:spring-boot-starter-mail")

    // db
    runtimeOnly("com.mysql:mysql-connector-j")
    testRuntimeOnly("com.h2database:h2")
    // logback & webclient
    api("io.netty:netty-all") // mac
    api("io.micrometer:micrometer-core") // mac

    //jwt
    api("io.jsonwebtoken:jjwt-api:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.2")

    //S3
    implementation("org.springframework.cloud:spring-cloud-starter-aws:2.2.5.RELEASE")
    implementation("org.apache.tika:tika-core:3.1.0")
}

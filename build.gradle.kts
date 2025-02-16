plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.jooqPlugin)
}
group = "lv.bogatirjovs"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation(libs.springBootStarterJooq)
    implementation(libs.springBootStarterWeb)
    implementation(libs.jacksonModuleKotlin)
    implementation(libs.kotlinReflect)

    // Lombok (compile-only)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // DB and jOOQ
    implementation(libs.jooqKotlin)
    runtimeOnly(libs.postgresql)
    jooqGenerator(libs.postgresql)

    //Tests
    testImplementation(libs.springBootStarterTest)
    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jooq {
    version.set(libs.versions.jooq.get())
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/currency"
                    user = "currency"
                    password = "currency"
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "currency"
                    }
                    target.apply {
                        packageName = "lv.bogatirjovs.currencycalculatorkotlin.jooq"
                        directory = "build/generated/jooq"
                    }
                }
            }
        }
    }
}

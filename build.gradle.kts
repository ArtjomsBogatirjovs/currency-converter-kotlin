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
    implementation(libs.jakartaPersistence)
    implementation(libs.jakartaValidation)

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
    testImplementation(libs.mockkDependency)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testImplementation(libs.testContainersJunit)
    testImplementation(libs.testContainersPostgres)
    implementation("org.apache.commons:commons-compress:1.27.1") //to avoid vulnerability from testContainers - delete in future
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}


val dbUrl = "jdbc:postgresql://localhost:5432/currency"
val dbUser = "currency"
val dbPassword = "currency"
val dbSchema = "currency"
val dbDriver = "org.postgresql.Driver"

jooq {
    version.set(libs.versions.jooq.get())
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(false)
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = dbDriver
                    url = dbUrl
                    user = dbUser
                    password = dbPassword
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = dbSchema
                    }
                    generate.apply {
                        isDeprecated = false
                        isValidationAnnotations = true
                        isJpaAnnotations = true
                        isPojos = true
                        isImmutablePojos = false
                        isFluentSetters = true
                        isDaos = true
                    }
                    target.apply {
                        packageName = "lv.bogatirjovs.currencycalculatorkotlin.jooq"
                        directory = "src/jooq/kotlin"
                    }
                }
            }
        }
    }
}

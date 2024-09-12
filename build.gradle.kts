buildscript {
    dependencies {
        classpath("${libs.testcontainers.postgresql.get()}")
        classpath("${libs.flyway.database.postgresql.get()}")
    }
}

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
    alias(libs.plugins.flyway)
    alias(libs.plugins.jooq)
}

group = "com.dnd"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Version들은 gradle/libs.versions.toml 파일에서 관리합니다.
    implementation(libs.bundles.spring.boot)
    implementation(libs.dotenv)
    implementation(libs.jetbrains.annotations)
    implementation(libs.springdoc)

    implementation(libs.bcpkix)

    //defect detection tool for Java that uses static analysis to look for more than 200 bug patterns
    implementation(libs.spotbugs)

    // JWT
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // Database
    implementation(libs.postgresql)
    runtimeOnly(libs.postgresql)
    implementation(libs.hibernate.spatial)

    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    // jOOQ
    implementation(libs.bundles.jooq)
    jooqCodegen(libs.postgresql)

    // Lombok
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(libs.bundles.testcontainers)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootBuildImage {
    createdDate = "now"
    doFirst {
        version = imageName.get().substringAfterLast(":", "Unknown")
        println("Setting version to: $version")
    }
}

springBoot.buildInfo { properties {} }

spotless {
    encoding = Charsets.UTF_8

    java {
        target("**/*.java")
        targetExclude("build/**", "**/*Request*.java", "**/*Response*.java", "**/*Dto*.java")
        palantirJavaFormat("2.47.0")
    }

    java {
        target("**/*.java")
        endWithNewline()
        indentWithSpaces(4)
        trimTrailingWhitespace()
        importOrder("", "java|javax", "\\#").wildcardsLast()
        removeUnusedImports()
    }

    format("misc") {
        // define the files to apply `misc` to
        target("*.gradle", "*.gradle.*", ".gitattributes", ".gitignore")

        // define the steps to apply to those files
        endWithNewline()
        indentWithSpaces(4)
        trimTrailingWhitespace()
    }
}

tasks.register<Copy>("updateGitHooks") {
    from("scripts/pre-commit.sh")
    into(".git/hooks")
    rename("pre-commit.sh", "pre-commit")
    doLast {
        val preCommitHook = file(".git/hooks/pre-commit")
        preCommitHook.setExecutable(true, false)
    }
}

tasks.compileJava {
    dependsOn("updateGitHooks")
    dependsOn(tasks.jooqCodegen)
}

tasks.register("projectTest") {
    dependsOn(tasks.spotlessCheck)
    dependsOn(tasks.compileJava)
    dependsOn(tasks.test)
}

// Here we register service for providing our database during the build.
val dbContainerProvider = project.gradle.sharedServices.registerIfAbsent("postgres", PostgresService::class) {}

tasks.flywayMigrate {
    usesService(dbContainerProvider)
    locations = arrayOf("filesystem:src/main/resources/db/migration")
    inputs.files(fileTree("src/main/resources/db/migration"))

    doFirst {
        val dbContainer = dbContainerProvider.get().container()
        url = dbContainer.jdbcUrl
        user = dbContainer.username
        password = dbContainer.password
    }
}

afterEvaluate {
    // For jOOQ to run we always need for flyway to be completed before.
    tasks.jooqCodegen {
        dependsOn(tasks.flywayMigrate)
        doFirst {
            val dbContainer = dbContainerProvider.get().container()
            jooq.configuration {
                jdbc = org.jooq.meta.jaxb.Jdbc().apply {
                    driver = "org.postgresql.Driver"
                    url = dbContainer.jdbcUrl
                    user = dbContainer.username
                    password = dbContainer.password
                }
            }
        }
        doLast {
            dbContainerProvider.orNull?.close()
        }
    }
}

jooq {
    configuration {
        logging = org.jooq.meta.jaxb.Logging.WARN
        generator {
            name = "org.jooq.codegen.DefaultGenerator"
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                includes = ".*"
                excludes = "flyway_schema_history"
                inputSchema = "public"
                forcedTypes {
                    forcedType {
                        userType = "com.dnd.runus.global.constant.MemberRole"
                        includeExpression = ".*\\.role"
                    }
                    forcedType {
                        userType = "com.dnd.runus.global.constant.SocialType"
                        includeExpression = ".*\\.social_type"
                    }
                    forcedType {
                        userType = "com.dnd.runus.global.constant.RunningEmoji"
                        includeExpression = ".*\\.emoji"
                    }
                }
            }
            target {
                packageName = "${group}.runus.jooq"
                directory = "target/generated-sources/jooq"
            }
        }
    }
}

// Build service for providing database container.
abstract class PostgresService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val container = org.testcontainers.containers.PostgreSQLContainer<Nothing>(
        org.testcontainers.utility.DockerImageName.parse(
            "imresamu/postgis:16-3.4-alpine"
        ).asCompatibleSubstituteFor("postgres")
    ).apply {
        start()
    }

    override fun close() = container.stop()

    fun container() = container
}

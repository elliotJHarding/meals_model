import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("java-library")
    id("maven-publish")
    id("org.openapi.generator") version "7.2.0"
}

group = "com.harding.meals"
version = "1.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

val openApiSpec = file("src/main/openapi/meals-contracts.yaml")
val generatedJavaDir = layout.buildDirectory.dir("generated/sources/openapi/java").get().asFile
val generatedTypeScriptDir = layout.buildDirectory.dir("generated/sources/openapi/typescript").get().asFile
val generatedPythonDir = layout.buildDirectory.dir("generated/sources/openapi/python").get().asFile

// Task to generate Java DTOs for backend (Spring Boot)
val generateJava by tasks.registering(GenerateTask::class) {
    generatorName.set("spring")
    inputSpec.set(openApiSpec.absolutePath)
    outputDir.set(generatedJavaDir.absolutePath)
    apiPackage.set("com.harding.meals.api")
    modelPackage.set("com.harding.meals.dto")
    invokerPackage.set("com.harding.meals.client")

    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "useJakartaEe" to "true",
        "interfaceOnly" to "true",
        "skipDefaultInterface" to "true",
        "useTags" to "true",
        "useSpringBoot3" to "true",
        "documentationProvider" to "none",
        "openApiNullable" to "false",
        "hideGenerationTimestamp" to "true",
        "additionalModelTypeAnnotations" to "@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)"
    ))

    // Generate only models, not full Spring controllers
    globalProperties.set(mapOf(
        "models" to "",
        "apis" to "false",
        "modelTests" to "false",
        "apiTests" to "false"
    ))
}

// Task to generate TypeScript interfaces for frontend (React)
val generateTypeScript by tasks.registering(GenerateTask::class) {
    generatorName.set("typescript-axios")
    inputSpec.set(openApiSpec.absolutePath)
    outputDir.set(generatedTypeScriptDir.absolutePath)

    configOptions.set(mapOf(
        "supportsES6" to "true",
        "modelPropertyNaming" to "camelCase",
        "enumPropertyNaming" to "UPPERCASE",
        "apiPackage" to "meals.api",
        "modelPackage" to "meals.model",
        "withInterfaces" to "true",
        "withSeparateModelsAndApi" to "true"
    ))

    additionalProperties.set(mapOf(
        "npmName" to "@elliotJHarding/meals-api",
        "npmVersion" to project.version.toString(),
        "snapshot" to "false"
    ))

    configOptions.put("npmRepository", "https://registry.npmjs.org")

    typeMappings.set(mapOf(
        "date" to "Date",
        "DateTime" to "Date",
    ))

    importMappings.set(mapOf(
        "date" to "Date",
        "DateTime" to "Date",
    ))

}

// Task to generate Python models for AI service (FastAPI)
val generatePython by tasks.registering(GenerateTask::class) {
    generatorName.set("python")
    inputSpec.set(openApiSpec.absolutePath)
    outputDir.set(generatedPythonDir.absolutePath)

    configOptions.set(mapOf(
        "packageName" to "meals_contract",
        "projectName" to "meals-contract"
    ))

    additionalProperties.set(mapOf(
        "packageVersion" to project.version.toString()
    ))
}

// Configure source sets to include generated Java code
sourceSets {
    main {
        java {
            srcDir("$generatedJavaDir/src/main/java")
        }
    }
}

tasks.named("compileJava") {
    dependsOn(generateJava)
}

// Task to copy generated TypeScript to a publishable location
val prepareTypeScriptPackage by tasks.registering(Copy::class) {
    dependsOn(generateTypeScript)
    from(generatedTypeScriptDir)
    into(layout.buildDirectory.dir("typescript-package"))

    doLast {
        val packageJson = layout.buildDirectory.file("typescript-package/package.json").get().asFile
        if (packageJson.exists()) {
            val content = packageJson.readText()

            // Update package.json with GitHub repository and publishConfig
            val updatedContent = content
                .replace(
                    "\"repository\": {",
                    "\"repository\": {\n    \"type\": \"git\",\n    \"url\": \"https://github.com/elliotJHarding/meals_model.git\"\n  },\n  \"publishConfig\": {\n    \"registry\": \"https://npm.pkg.github.com\"\n  },\n  \"_oldRepository\": {"
                )
            packageJson.writeText(updatedContent)
        }

        // Create .npmrc for GitHub Packages authentication
        val npmrc = layout.buildDirectory.file("typescript-package/.npmrc").get().asFile
        npmrc.writeText("""
            @elliotJHarding:registry=https://npm.pkg.github.com
            //npm.pkg.github.com/:_authToken=${'$'}{NODE_AUTH_TOKEN}
        """.trimIndent())
    }
}

// Task to prepare Python package
val preparePythonPackage by tasks.registering(Copy::class) {
    dependsOn(generatePython)
    from(generatedPythonDir)
    into(layout.buildDirectory.dir("python-package"))
}

tasks.named("build") {
    dependsOn(prepareTypeScriptPackage, preparePythonPackage)
}

// Publishing configuration for Java artifacts
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.harding.meals"
            artifactId = "meals-contract"
            version = project.version.toString()
        }
    }

    repositories {
        // Local Maven repository for development
        mavenLocal()

        // GitHub Packages for CI/CD
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/elliotJHarding/meals_model")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    // Spring Boot for Java generation
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")
    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")
}

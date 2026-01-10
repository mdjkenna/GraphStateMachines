import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

data class GsmUserAndPassword(
    val gsmUser : String?, 
    val gsmPassword : String?,
    val signingKey: String?,
    val signingPassword: String?
)

val gsmUserAndPassword : GsmUserAndPassword by lazy {
    println("Loading credentials...")

    val localPropertiesFile = rootProject.file("local.properties")
    val properties = if (localPropertiesFile.exists()) {
        Properties().apply {
            FileInputStream(localPropertiesFile).use { load(it) }
        }.also {
            it.forEach { (key, value) -> extra[key.toString()] = value }
        }
    } else {
        println("local.properties file not found")
        null
    }

    fun getCredential(envVar: String, propKey: String, description: String): String? {
        val envValue = System.getenv(envVar)
        if (!envValue.isNullOrBlank()) {
            println("$description: Using environment variable $envVar")
            return envValue
        }
        
        val propValue = properties?.get(propKey) as? String
        if (!propValue.isNullOrBlank()) {
            println("$description: Using local.properties $propKey")
            return propValue
        }
        
        println("$description: Not found (checked $envVar and $propKey)")
        return null
    }
    
    fun getSigningKey(): String? {
        val envValue = System.getenv("SIGNING_KEY")
        if (!envValue.isNullOrBlank()) {
            println("Signing Key: Using environment variable SIGNING_KEY")
            return envValue
        }
        
        val signingKeyFile = properties?.get("SIGNING_KEY") as? String
        if (!signingKeyFile.isNullOrBlank()) {
            val keyFile = rootProject.file(signingKeyFile)
            if (keyFile.exists()) {
                println("Signing Key: Reading from file $signingKeyFile")
                return keyFile.readText()
            } else {
                println("Signing Key: File $signingKeyFile not found")
            }
        }
        
        println("Signing Key: Not found (checked SIGNING_KEY env var and SIGNING_KEY_FILE property)")
        return null
    }

    // Prefer Central Portal User Tokens; fall back to legacy names if present
    fun getCredentialAny(envVars: List<String>, propKeys: List<String>, description: String): String? {
        envVars.forEach { env ->
            val v = System.getenv(env)
            if (!v.isNullOrBlank()) {
                println("$description: Using environment variable $env")
                return v
            }
        }
        propKeys.forEach { key ->
            val v = properties?.get(key) as? String
            if (!v.isNullOrBlank()) {
                println("$description: Using local.properties $key")
                return v
            }
        }
        println("$description: Not found (checked ${envVars.joinToString()} and ${propKeys.joinToString()})")
        return null
    }

    val gsmUser = getCredentialAny(
        envVars = listOf("CENTRAL_TOKEN_USERNAME", "OSSRH_USERNAME", "GSM_USER"),
        propKeys = listOf("CENTRAL_TOKEN_USERNAME", "OSSRH_USERNAME", "GSM_USER"),
        description = "Central Portal Token Username"
    )
    val gsmPassword = getCredentialAny(
        envVars = listOf("CENTRAL_TOKEN_PASSWORD", "OSSRH_PASSWORD", "GSM_PASSWORD"),
        propKeys = listOf("CENTRAL_TOKEN_PASSWORD", "OSSRH_PASSWORD", "GSM_PASSWORD"),
        description = "Central Portal Token Password"
    )
    val signingKey = getSigningKey()
    val signingPassword = getCredential("SIGNING_PASSWORD", "SIGNING_PASSWORD", "Signing Password")

    println("📋 Credential Summary:")
    println("   Central Token Username: ${if (gsmUser != null) "Available" else "Missing"}")
    println("   Central Token Password: ${if (gsmPassword != null) "Available" else "Missing"}")
    println("   Signing Key: ${if (signingKey != null) "Available (${signingKey.length} chars)" else "Missing"}")
    println("   Signing Password: ${if (signingPassword != null) "Available" else "Missing (will use empty)"}")

    GsmUserAndPassword(
        gsmUser,
        gsmPassword,
        signingKey,
        signingPassword
    )
}

object Props {
    const val VERSION = "1.0.0"
}

plugins {
    kotlin("jvm") version "2.1.0"
    id("jacoco")
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.9.0"
}

jacoco {
    toolVersion = "0.8.10"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

repositories {
    mavenCentral()
}

group = "io.github.mdjkenna"
version = Props.VERSION

tasks.test {
   useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaJavadoc"))
    archiveClassifier.set("javadoc")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-framework-datatest:5.8.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            includeNonPublic.set(false)
            skipDeprecated.set(true)
            reportUndocumented.set(true)
            jdkVersion.set(8)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "graph-state-machines"
            version = Props.VERSION

            from(components["java"])

            pom {
                name.set("GraphStateMachines")
                description.set("A Kotlin library for creating graph based state machines")
                url.set("https://github.com/mdjkenna/GraphStateMachines")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("mdjkenna")
                        name.set("mdjkenna")
                        email.set("mdjkenna5813@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/mdjkenna/GraphStateMachines.git")
                    developerConnection.set("scm:git:ssh://git@github.com/mdjkenna/GraphStateMachines.git")
                    url.set("https://github.com/mdjkenna/GraphStateMachines")
                }
            }
        }
    }

    repositories {
        maven {
            name = "LocalTest"
            url = uri("file://${layout.buildDirectory.get()}/repo")
        }

        maven {
            name = "OSSRH"
            // As of 2025: Central's OSSRH Staging API compatibility service for releases,
            // and Central's snapshots repository for -SNAPSHOT builds.

            val releasesRepoUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) {
                snapshotsRepoUrl
            } else {
                releasesRepoUrl
            }

            credentials {
                username = gsmUserAndPassword.gsmUser
                password = gsmUserAndPassword.gsmPassword
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

/**
 * https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/
 * */
tasks.register("centralPortalUpload") {
    group = "publishing"
    description = "Notify Central Portal to ingest the staged repository for this namespace"

    doLast {
        if (version.toString().endsWith("SNAPSHOT")) {
            println("Skipping Central Portal upload for -SNAPSHOT version")
            return@doLast
        }

        val user = gsmUserAndPassword.gsmUser
        val pass = gsmUserAndPassword.gsmPassword
        require(!user.isNullOrBlank() && !pass.isNullOrBlank()) {
            "Central Portal token username/password are required"
        }

        val namespace = project.group.toString()
        val urlStr = "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$namespace?publishing_type=automatic"
        println("Notifying Central Portal: POST $urlStr")

        val bearer = Base64.getEncoder()
            .encodeToString("$user:$pass".toByteArray(Charsets.UTF_8))

        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $bearer")
            doOutput = true
        }

        conn.outputStream.use { /* empty body */ }
        val code = conn.responseCode
        val msg = conn.responseMessage

        println("Central Portal response: $code $msg")

        if (code >= 300) {
            val err = conn.errorStream?.readBytes()?.toString(Charsets.UTF_8)
            println("defaultRepository upload failed ($code). Falling back to search + repository upload...\n$err")

            val searchUrlStr = "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?ip=any&profile_id=$namespace"
            println("Searching for staging repositories: GET $searchUrlStr")
            val searchConn = (URL(searchUrlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $bearer")
            }

            val searchCode = searchConn.responseCode
            val searchBody = (if (searchCode < 300) searchConn.inputStream else searchConn.errorStream)?.readBytes()?.toString(Charsets.UTF_8)
            println("Search response: $searchCode\n$searchBody")

            if (searchCode >= 300 || searchBody.isNullOrBlank()) {
                throw GradleException("Central Portal search failed: $searchCode\n$searchBody")
            }

            val regex = "\\\"repositoryId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"".toRegex()
            val repoId = regex.find(searchBody)?.groupValues?.getOrNull(1)
            require(!repoId.isNullOrBlank()) { "Could not locate a repositoryId in search response" }
            val uploadRepoUrlStr = "https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/$repoId?publishing_type=automatic"
            println("Uploading repository to Portal: POST $uploadRepoUrlStr")

            val uploadConn = (URL(uploadRepoUrlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $bearer")
                doOutput = true
            }

            uploadConn.outputStream.use { }

            val uploadCode = uploadConn.responseCode
            val uploadMsg = uploadConn.responseMessage
            val uploadErr = (if (uploadCode < 300) uploadConn.inputStream else uploadConn.errorStream)?.readBytes()?.toString(Charsets.UTF_8)
            println("Upload repository response: $uploadCode $uploadMsg\n$uploadErr")

            if (uploadCode >= 300) {
                throw GradleException("Central Portal repository upload failed: $uploadCode $uploadMsg\n$uploadErr")
            }

        }
    }
}

tasks.register("publishToCentral") {
    group = "publishing"
    description = "Publish to Central (OSSRH Staging API) and notify Portal"
    dependsOn("publishMavenJavaPublicationToOSSRHRepository")
    finalizedBy("centralPortalUpload")
}

signing {
    val shouldSign = !gsmUserAndPassword.signingKey.isNullOrBlank()
    
    if (shouldSign) {
        useInMemoryPgpKeys(gsmUserAndPassword.signingKey, gsmUserAndPassword.signingPassword ?: "")
        sign(publishing.publications["mavenJava"])
    }

    isRequired = true
}


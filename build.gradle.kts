import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChildren
import org.jreleaser.model.*
import java.net.URI

val env = System.getenv()!!
val isCiEnv = env["CI"].toBoolean()
val gpgKeyPassphrase = env["GPG_PASSPHRASE_KEY"]
val gpgKeyPublic = env["GPG_PUBLIC_KEY"]
val gpgKeyPrivate = env["GPG_PRIVATE_KEY"]
val mavenUsername = env["MAVEN_USERNAME"]
val mavenPassword = env["MAVEN_PASSWORD"]

if (!isCiEnv) {
    version = "${version}.local"
}

buildscript {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
    }

    dependencies {
        classpath(libs.enigma.cli)
    }
}

plugins {
	`java-library`
    `maven-publish`

    alias(libs.plugins.spotless)

    alias(libs.plugins.jreleaser)
    alias(libs.plugins.gradle.plugin.publish)
}

repositories {
	maven {
		name = "Fabric"
		url = uri("https://maven.fabricmc.net/")
	}
	mavenCentral()
}

dependencies {
    implementation(libs.bundles.asm)
    implementation(libs.bundles.enigma)
    implementation(libs.bundles.unpick)

    implementation(libs.javapoet)
    implementation(libs.mappingio)
    implementation(libs.tinyremapper)

    // Contains a number of useful utilities we can re-use.
    implementation(libs.loom) {
        isTransitive = false
    }

    testImplementation(libs.assertj.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    test {
        useJUnitPlatform()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

/*
 * A task to ensure that the version being released has not already been released.
 */
val checkVersion by tasks.registering {
    doFirst {
        val xml = URI(
            "https://repo.maven.apache.org/maven2/${
                rootProject.group.toString().replace(".", "/")
            }/${rootProject.name}/maven-metadata.xml"
        ).toURL().readText()
        val metadata = XmlSlurper().parseText(xml)

        val versioning = metadata.getProperty("versioning") as GPathResult
        val versions = versioning.getProperty("versions") as GPathResult
        val versionText = (versions.getProperty("version") as NodeChildren).map { it.toString() }

        if (versionText.contains(version)) {
            throw RuntimeException("$version has already been released!")
        }
    }
}

gradlePlugin {
    website = property("url").toString()
    vcsUrl = property("url").toString()

	plugins {
		create("filament") {
			id = "${rootProject.group}.${rootProject.name}"
            displayName = rootProject.name
            tags = (listOf("projectzomboid", "zomboid", "leaf"))
			implementationClass = "${rootProject.group}.${rootProject.name}.FilamentGradlePlugin"
		}
	}
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = rootProject.name
            group = rootProject.group
            description = rootProject.description
            url = property("url").toString()
            inceptionYear = "2025"

            developers {
                developer {
                    id = "aoqia"
                    name = "aoqia"
                }
            }

            issueManagement {
                system = "GitHub"
                url = "${property("url").toString()}/issues"
            }

            licenses {
                license {
                    name = "CC0-1.0"
                    url = "https://spdx.org/licenses/CC0-1.0.html"
                }
            }

            scm {
                connection = "scm:git:${property("url").toString()}.git"
                developerConnection =
                    "scm:git:${property("url").toString().replace("https", "ssh")}.git"
                url = property("url").toString()
            }
        }
    }

    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    project {
        name = rootProject.name
        version = rootProject.version.toString()
        versionPattern = "SEMVER"
        authors = listOf("aoqia194", "FabricMC")
        maintainers = listOf("aoqia194")
        license = "CC0-1.0"
        inceptionYear = "2025"

        links {
            homepage = property("url").toString()
            license = "https://spdx.org/licenses/MIT.html"
        }
    }

    signing {
        active = Active.ALWAYS
        armored = true
        passphrase = gpgKeyPassphrase
        publicKey = gpgKeyPublic
        secretKey = gpgKeyPrivate
    }

    deploy {
        maven {
            pomchecker {
                version = "1.14.0"
                failOnWarning = false // annoying
                failOnError = true
                strict = true
            }

            mavenCentral {
                create("sonatype") {
                    applyMavenCentralRules = true
                    active = Active.ALWAYS
                    snapshotSupported = true
                    authorization = Http.Authorization.BEARER
                    username = mavenUsername
                    password = mavenPassword
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                    verifyUrl = "https://repo1.maven.org/maven2/{{path}}/{{filename}}"
                    namespace = rootProject.group.toString()
                    retryDelay = 60
                    maxRetries = 30

                    // Override the plugin marker artifact to disable maven jar checks.
                    artifactOverride {
                        groupId = "${rootProject.group}.${rootProject.name}"
                        artifactId = "${rootProject.group}.${rootProject.name}.gradle.plugin"
                        jar = false
                        sourceJar = false
                        javadocJar = false
                        verifyPom = true
                    }
                }
            }
        }
    }

    release {
        github {
            enabled = true
            repoOwner = "aoqia194"
            name = "leaf-${rootProject.name}"
            host = "github.com"
            releaseName = "{{tagName}}"

            sign = true
            overwrite = true
            uploadAssets = Active.ALWAYS
            artifacts = true
            checksums = true
            signatures = true

            changelog {
                formatted = Active.ALWAYS
                preset = "conventional-commits"
                extraProperties.put("categorizeScopes", "true")
            }
        }
    }
}

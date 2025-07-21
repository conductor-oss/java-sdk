package io.orkes.conductor.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.GradleException
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.plugins.signing.SigningPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.aws.AwsImAuthentication
import org.gradle.api.tasks.Exec


class PublishConfigPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.withType(MavenPublishPlugin) {
            publishingConfig(project)
        }
        project.plugins.withType(SigningPlugin) {
            signingConfig(project)
        }
        
        // Add Maven Central promotion task
        addMavenCentralPromotionTask(project)
    }

    def publishingConfig(Project project) {
        project.publishing {
            publications(publicationConfig(project))
            repositories(repositoriesConfig(project))
        }
    }

    def signingConfig(Project project) {
        project.signing {
            def signingKeyId = project.findProperty('signingKeyId')
            if (signingKeyId) {
                def signingKey = project.findProperty('signingKey')
                def signingPassword = project.findProperty('signingPassword')
                if (signingKeyId && signingKey && signingPassword) {
                    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
                }
                sign project.publishing.publications
            }
        }
    }

    def addMavenCentralPromotionTask(Project project) {
        project.task('promoteToMavenCentral') {
            group = 'publishing'
            description = 'Promotes staged artifacts to Maven Central'
            
            onlyIf {
                project.hasProperty('mavenCentral') && 
                project.hasProperty('username') && 
                project.hasProperty('password') &&
                !project.version.endsWith('-SNAPSHOT')
            }
            
            doLast {
                def username = project.properties['username']
                def password = project.properties['password']
                
                // Create base64 encoded token for authentication
                def token = "${username}:${password}".bytes.encodeBase64().toString()
                
                // Get open staging repositories
                def response = new URL("https://ossrh-staging-api.central.sonatype.com/manual/search/repositories")
                    .openConnection()
                response.setRequestProperty("Authorization", "Basic ${token}")
                response.setRequestProperty("Content-Type", "application/json")
                
                def repositories = new groovy.json.JsonSlurper().parse(response.inputStream)
                
                // Promote each open repository
                repositories.repositories.each { repo ->
                    if (repo.state == "open") {
                        project.logger.lifecycle("Promoting repository ${repo.key}")
                        
                        def promoteUrl = new URL("https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/${repo.key}?publishing_type=automatic")
                        def connection = promoteUrl.openConnection()
                        connection.setRequestMethod("POST")
                        connection.setRequestProperty("Authorization", "Basic ${token}")
                        connection.setRequestProperty("Content-Type", "application/json")
                        
                        def responseCode = connection.responseCode
                        if (responseCode == 200) {
                            project.logger.lifecycle("Successfully promoted repository ${repo.key}")
                        } else {
                            def errorMessage = "Failed to promote repository ${repo.key}. Response code: ${responseCode}"
                            project.logger.error(errorMessage)
                            throw new GradleException(errorMessage)
                        }
                    }
                }
            }
        }
        
        project.tasks.matching { it.name == 'publish' }.configureEach { publishTask ->
            publishTask.finalizedBy project.tasks.promoteToMavenCentral
        }
    }

    def publicationConfig(Project project) {
        return {
            mavenJava(MavenPublication) {
                if (project.hasProperty('artifactId')) {
                    artifactId = project.findProperty('artifactId')
                }

                from project.components.java
                pom {
                    name = project.findProperty('artifactName')
                    description = project.findProperty('artifactDescription')
                    url = 'https://github.com/conductor-oss/conductor-java-sdk.git'
                    scm {
                        connection = 'scm:git:git://github.com/conductor-oss/conductor-java-sdk.git'
                        developerConnection = 'scm:git:ssh://github.com/conductor-oss/conductor-java-sdk.git'
                        url = 'https://github.com/conductor-oss/conductor-java-sdk.git'
                    }
                    licenses {
                        license {
                            name = 'The Apache License, Version 2.0'
                            url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                        }
                    }
                    developers {
                        developer {
                            organization = 'Orkes'
                            organizationUrl = 'https://orkes.io'
                            name = 'Orkes Development Team'
                            email = 'developers@orkes.io'
                        }
                    }
                }
            }
        }
    }

    def repositoriesConfig(Project project) {
        return {
            maven {
                if (project.hasProperty("mavenCentral")) {
                    url = getMavenRepoUrl(project)
                    credentials {
                        username = project.properties['username']
                        password = project.properties['password']
                    }
                } else {
                    url = getS3BucketUrl(project)
                    authentication {
                        awsIm(AwsImAuthentication)
                    }
                }
            }
        }
    }

    static String getS3BucketUrl(Project project) {
        return "s3://orkes-artifacts-repo/${project.version.endsWith('-SNAPSHOT') ? 'snapshots' : 'releases'}"
    }

    static String getMavenRepoUrl(Project project) {
        return "https://ossrh-staging-api.central.sonatype.com/${project.version.endsWith('-SNAPSHOT') ? 'content/repositories/snapshots/' : 'service/local/staging/deploy/maven2/'}"
    }
}

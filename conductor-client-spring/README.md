# Conductor Client Spring for Spring Boot 3

Spring Boot 3 auto-configuration for the Conductor core client, workflow SDK, and Java workers.

**Prerequisites:** Java 21+, Spring Boot 3.5.13+ (see [Older Spring Boot 3 versions](#older-spring-boot-3-versions)), and a running [OSS or Orkes Conductor server](../docs/connection-authentication.md). For Spring Boot 4, use [the Boot 4 module](../conductor-client-spring-boot4/README.md).

## Install

Use the published version from [Maven Central](https://search.maven.org/artifact/org.conductoross/conductor-client-spring).

```groovy
implementation 'org.conductoross:conductor-client-spring:<VERSION>'
```

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client-spring</artifactId>
    <version>&lt;VERSION&gt;</version>
</dependency>
```

### Older Spring Boot 3 versions

The SDK uses Jackson 3, which needs `com.fasterxml.jackson.core:jackson-annotations` 2.21 or later. Spring Boot's dependency management picks that version for your app, and Spring Boot 3.5.12 and earlier pick an older one. The app then fails at startup with `Conductor SDK could not start Jackson: missing class com/fasterxml/jackson/annotation/...`.

Upgrade to Spring Boot 3.5.13 or later, or override Boot's Jackson version:

```xml
<!-- Maven, with spring-boot-starter-parent -->
<properties>
    <jackson-bom.version>2.21.4</jackson-bom.version>
</properties>
```

```groovy
// Gradle, with the io.spring.dependency-management plugin
ext['jackson-bom.version'] = '2.21.4'
```

## Configure

```properties
conductor.client.root-uri=http://localhost:8080/api
conductor.client.verifying-ssl=true
```

For Orkes, provide the endpoint and credentials through your deployment's environment/secret manager; see [connection and authentication](../docs/connection-authentication.md). Never store auth credentials in `application.properties` committed to source control.

## What auto-configuration provides

With `@SpringBootApplication`, the module discovers its auto-configurations automatically. It creates a `ConductorClient`, `TaskClient`, `WorkflowClient`, `WorkflowExecutor`, `AnnotatedWorkerExecutor`, and a managed `TaskRunnerConfigurer` when the required beans are available. No `@ComponentScan` for `com.netflix.conductor` is required.

Spring discovers `Worker` beans and `@Component` beans with public `@WorkerTask` methods. The task runner starts with the application and calls `shutdown()` during graceful Spring shutdown.

```java
@Component
class GreetingTasks {
    @WorkerTask("greet")
    public @OutputParam("greeting") String greet(@InputParam("name") String name) {
        return "Hello, " + name;
    }
}
```

**Expected result:** when a registered workflow reaches `greet`, the managed worker polls it and completes the task. Set per-task worker thread counts with `conductor.worker.<task-name>.threadCount`; keep side effects idempotent.

## Customization and migration

Define your own `ConductorClient`, `TaskClient`, `WorkflowClient`, `WorkflowExecutor`, or `TaskRunnerConfigurer` bean to replace the corresponding default. Use `conductor.client.base-path` as an alternative to `root-uri`; `root-uri` wins when both are present.

Older documentation showed Java 17 and a fixed `4.0.0` coordinate. This module now follows the Java 21 SDK baseline and uses `<VERSION>` from Maven Central. It also uses Boot auto-configuration, so remove legacy manual component scanning unless it serves your own application components.

Next: [Spring integration selector](../docs/spring-boot.md), [workers](../docs/workers.md), and [reliability](../docs/reliability.md).

# Conductor Client Spring for Spring Boot 4

Spring Boot 4 / Spring Framework 7 auto-configuration for the Conductor core client, workflow SDK, and Java workers.

**Prerequisites:** Java 21+, Spring Boot 4, and a running [OSS or Orkes Conductor server](../docs/connection-authentication.md). For Spring Boot 3, use [the Boot 3 module](../conductor-client-spring/README.md).

## Install

Use the published version from [Maven Central](https://search.maven.org/artifact/org.conductoross/conductor-client-spring-boot4).

```groovy
implementation 'org.conductoross:conductor-client-spring-boot4:<VERSION>'
```

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client-spring-boot4</artifactId>
    <version>&lt;VERSION&gt;</version>
</dependency>
```

## Configure

```properties
conductor.client.root-uri=http://localhost:8080/api
conductor.client.verifying-ssl=true
```

For Orkes, inject the endpoint and credentials with your platform secret manager; see [connection and authentication](../docs/connection-authentication.md). Never commit credentials to an application configuration file.

## What auto-configuration provides

The module is registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. A normal `@SpringBootApplication` discovers it automatically; no manual `@ComponentScan` is needed.

When `conductor.client.root-uri` or `conductor.client.base-path` is configured, it provides a `ConductorClient`, `TaskClient`, `WorkflowClient`, `WorkflowExecutor`, `AnnotatedWorkerExecutor`, and a `TaskRunnerConfigurer` whose lifecycle is managed by Spring. `Worker` beans and public `@WorkerTask` methods on `@Component` beans are registered for polling.

```java
@Component
class GreetingTasks {
    @WorkerTask("greet")
    public @OutputParam("greeting") String greet(@InputParam("name") String name) {
        return "Hello, " + name;
    }
}
```

**Expected result:** a registered `greet` task is polled by the Spring-managed worker and completes. Set `conductor.worker.greet.threadCount` to control its worker concurrency, and make external effects idempotent.

## Migration from Boot 3

Switch only the dependency artifact from `conductor-client-spring` to `conductor-client-spring-boot4`; the `conductor.client.*` configuration and worker annotations are intentionally the same. Verify Java 21 and your Boot 4 dependency set first. If you supplied custom client/runner beans, retain them: `@ConditionalOnMissingBean` keeps your implementations in control.

Next: [Spring integration selector](../docs/spring-boot.md), [workers](../docs/workers.md), and [deployment/scaling](../docs/deployment-scaling.md).

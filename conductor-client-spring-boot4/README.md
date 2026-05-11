# Conductor Client Spring (Spring Boot 4)

Provides Spring Boot 4 / Spring Framework 7 auto-configurations for the Conductor client and SDK.

For Spring Boot 3 consumers, use `org.conductoross:conductor-client-spring` instead.

## Getting Started

### Prerequisites
- Java 21 or higher
- A Spring Boot 4 project
- A running Conductor server (local or remote)

### Usage

Add the dependency:

```groovy
implementation 'org.conductoross:conductor-client-spring-boot4:<version>'
```

The auto-configurations are registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and discovered automatically by `@SpringBootApplication`. No `@ComponentScan` is required.

Configure the client:

```properties
conductor.client.root-uri=http://localhost:8080/api
```

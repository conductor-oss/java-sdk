# Compatibility matrix

**Audience:** teams selecting modules and runtime baselines.  
**Last verified:** repository build and module configuration.

| Area | Supported baseline | Notes |
|---|---|---|
| Java SDK | Java 21+ | This repository compiles and tests with Java 21. |
| OSS Conductor | Supported server deployment | Use the CI server matrix as the exercised compatibility baseline; test your target server during an upgrade. |
| Orkes | Supported tenant API | Set the tenant API endpoint and credentials; availability of enterprise features is tenant-specific. |
| Spring Boot 3 | Java 17+ / Spring Boot 3 | Use `conductor-client-spring`; the SDK itself remains Java 21+. |
| Spring Boot 4 | Java 21+ / Spring Boot 4 | Use `conductor-client-spring-boot4`. |

## Published modules

| Module | Purpose | Maven Central | Javadocs |
|---|---|---|---|
| `conductor-client` | Core workflow, task, metadata, file, and scheduler clients | [artifact](https://search.maven.org/artifact/org.conductoross/conductor-client) | [Javadocs](https://javadoc.io/doc/org.conductoross/conductor-client) |
| `conductor-client-ai` | Agent runtime, definitions, tools, and bridges | [artifact](https://search.maven.org/artifact/org.conductoross/conductor-client-ai) | [Javadocs](https://javadoc.io/doc/org.conductoross/conductor-client-ai) |
| `conductor-client-ai-spring` | Spring support for agents | [artifact](https://search.maven.org/search?q=g:org.conductoross%20AND%20a:conductor-client-ai-spring) | Source and generated artifact Javadocs when published |
| `conductor-client-spring` | Spring Boot 3 core-client auto-configuration | [artifact](https://search.maven.org/artifact/org.conductoross/conductor-client-spring) | [Javadocs](https://javadoc.io/doc/org.conductoross/conductor-client-spring) |
| `conductor-client-spring-boot4` | Spring Boot 4 core-client auto-configuration | [artifact](https://search.maven.org/artifact/org.conductoross/conductor-client-spring-boot4) | [Javadocs](https://javadoc.io/doc/org.conductoross/conductor-client-spring-boot4) |
| `conductor-client-metrics` | SDK metrics integration | [artifact](https://search.maven.org/artifact/org.conductoross/conductor-client-metrics) | [Javadocs](https://javadoc.io/doc/org.conductoross/conductor-client-metrics) |

Use `<VERSION>` from [Maven Central](https://search.maven.org/search?q=g:org.conductoross), never a README-pinned release. Generated Javadocs are the signature reference; linked source remains the fallback for snapshot work.

Next: [Spring integration](spring-boot.md), [API map](api-map.md), and [upgrading](upgrading.md).

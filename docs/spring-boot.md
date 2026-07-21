# Spring Boot integration selector

**Audience:** Spring applications that run Conductor clients or workers.  
**Works with:** OSS and Orkes.

| Your application | Module | Java baseline | Guide |
|---|---|---|---|
| Spring Boot 3 | `conductor-client-spring` | Java 17+ for Boot; Java 21+ for this SDK | [Boot 3](../conductor-client-spring/README.md) |
| Spring Boot 4 | `conductor-client-spring-boot4` | Java 21+ | [Boot 4](../conductor-client-spring-boot4/README.md) |
| AI agents in Spring | `conductor-client-ai-spring` | match your Spring application | [agent Spring guide](agents/spring-boot.md) |

Both core modules use Spring Boot auto-configuration. Add the matching dependency, configure `conductor.client.root-uri`, and let `@SpringBootApplication` discover the client/worker configuration. Do not add manual component scanning for Conductor packages unless your application has a separate reason.

Expected result: Spring creates the configured client beans and starts annotated worker beans through the managed task runner. See each module README for properties, custom beans, worker discovery, and migration details.

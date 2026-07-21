# Upgrade the Java SDK safely

**Audience:** teams upgrading the SDK, Java baseline, or workflow definitions.  
**Works with:** OSS and Orkes.

## Before the upgrade

1. Select a published `<VERSION>` from [Maven Central](https://search.maven.org/search?q=g:org.conductoross) and read its release notes.
2. Confirm Java 21 for SDK applications and select the matching Spring module: [Boot 3](../conductor-client-spring/README.md) or [Boot 4](../conductor-client-spring-boot4/README.md).
3. Compile and run your worker and agent test suites against the target server version.
4. Review deprecated APIs in the generated Javadocs and migrate before removing the old version.

## Roll out safely

Deploy new workers before definitions that require their task types. Keep worker implementations backward compatible while old executions are in flight. For breaking workflow changes, publish a new workflow version, route new starts to it, and drain old executions before removing the old version.

Do not change task names, output keys, tool contracts, or credential scopes in place unless all callers and in-flight executions remain compatible.

## Verify and roll back

Monitor workflow completion, retries, queue age, and error reasons after rollout. A rollback should restore a compatible worker/definition pair; never delete definitions needed by active executions.

Next: [workflow lifecycle](workflow-lifecycle.md), [compatibility](compatibility.md), and [debugging](debugging.md).

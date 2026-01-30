# Conductor Java SDK
[![Build Status](https://github.com/conductor-oss/java-sdk/actions/workflows/ci.yml/badge.svg)](https://github.com/conductor-oss/java-sdk/actions/workflows/ci.yml)

Java SDK for working with https://github.com/conductor-oss/conductor.

[Conductor](https://www.conductor-oss.org/) is the leading open-source orchestration platform allowing developers to build highly scalable distributed applications.

Check out the [official documentation for Conductor](https://orkes.io/content).

## ⭐ Conductor OSS

Show support for the Conductor OSS.  Please help spread the awareness by starring Conductor repo.

[![GitHub stars](https://img.shields.io/github/stars/conductor-oss/conductor.svg?style=social&label=Star&maxAge=)](https://GitHub.com/conductor-oss/conductor/)

## Content
<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Install Conductor Java SDK](#install-conductor-java-sdk)
  - [Get Conductor Java SDK](#get-conductor-java-sdk)
- [🚀 Quick Start](#-quick-start)
- [📚 Documentation](#-documentation)
- [Hello World Application Using Conductor](#hello-world-application-using-conductor)
  - [Step 1: Create Workflow](#step-1-create-workflow)
  - [Step 2: Write Task Worker](#step-2-write-task-worker)
  - [Step 3: Write _Hello World_ Application](#step-3-write-_hello-world_-application)
- [Running Workflows on Conductor Standalone (Installed Locally)](#running-workflows-on-conductor-standalone-installed-locally)
  - [Setup Environment Variable](#setup-environment-variable)
  - [Start Conductor Server](#start-conductor-server)
  - [Execute Hello World Application](#execute-hello-world-application)
- [Running Workflows on Orkes Conductor](#running-workflows-on-orkes-conductor)
- [Learn More about Conductor Java SDK](#learn-more-about-conductor-java-sdk)
- [Writing Workers](#writing-workers)
  - [Implementing Workers](#implementing-workers)
  - [Managing Workers in Application](#managing-workers-in-application)
  - [Design Principles for Workers](#design-principles-for-workers)
  - [Worker vs. Microservice/HTTP Endpoints](#worker-vs-microservicehttp-endpoints)
  - [Deploying Workers in Production](#deploying-workers-in-production)
- [Create Conductor Workflows](#create-conductor-workflows)
  - [Creating Workflows](#creating-workflows)
  - [Executing Workflows](#executing-workflows)
  - [Managing Workflow Executions](#managing-workflow-executions)
  - [Searching for Workflows](#searching-for-workflows)
  - [Handling Failures, Retries and Rate Limits](#handling-failures-retries-and-rate-limits)
- [Using Conductor in Your Application](#using-conductor-in-your-application)
  - [Adding Conductor SDK to Your Application](#adding-conductor-sdk-to-your-application)
  - [Testing Workflows](#testing-workflows)
  - [Workflow Deployments Using CI/CD](#workflow-deployments-using-cicd)
  - [Versioning Workflows](#versioning-workflows)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Install Conductor Java SDK

Before installing Conductor Java SDK, it is a good practice to ensure you have Java 11 or higher installed:

```shell
java -version
```

### Get Conductor Java SDK

The SDK requires Java 11+. To use the SDK, add the following dependency to your project:

**For Gradle:**

```gradle
dependencies {
    implementation 'org.conductoross:conductor-client:4.2.0'
    implementation 'org.conductoross:java-sdk:4.2.0'
}
```

**For Maven:**

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>4.2.0</version>
</dependency>
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>java-sdk</artifactId>
    <version>4.2.0</version>
</dependency>
```

## 🚀 Quick Start

For a complete end-to-end example, see the [examples](examples/) directory.

```bash
export CONDUCTOR_SERVER_URL="http://localhost:8080/api"
# See examples/README.md for running examples
```

This demonstrates:
- Registering a workflow definition
- Starting workflow execution  
- Running workers
- Monitoring workflow execution

## 📚 Documentation

**Getting Started:**
- **[Examples](examples/README.md)** - Complete examples with quick reference
- **[Conductor Client](conductor-client/README.md)** - HTTP client library documentation

**Worker Documentation:**
- **[Worker SDK](java-sdk/worker_sdk.md)** - Complete worker framework guide
- **[Workflow SDK](java-sdk/workflow_sdk.md)** - Workflow-as-code documentation
- **[Testing Framework](java-sdk/testing_framework.md)** - Unit testing workflows and workers

**Monitoring & Advanced:**
- **[Client Metrics](conductor-client-metrics/README.md)** - Metrics collection
- **[Spring Integration](conductor-client-spring/README.md)** - Spring Boot auto-configuration

## Hello World Application Using Conductor

In this section, we will create a simple "Hello World" application that executes a "greetings" workflow managed by Conductor.

### Step 1: Create Workflow

Create `GreetingsWorkflow.java`:

```java
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.SimpleTask;

public class GreetingsWorkflow {
    public ConductorWorkflow<WorkflowInput> createWorkflow() {
        ConductorWorkflow<WorkflowInput> workflow = new ConductorWorkflow<>(workflowExecutor);
        workflow.setName("greetings");
        workflow.setVersion(1);
        
        SimpleTask greetingsTask = new SimpleTask("greet", "greet_ref");
        greetingsTask.input("name", "${workflow.input.name}");
        
        workflow.add(greetingsTask);
        return workflow;
    }
}
```

### Step 2: Write Task Worker

Write a worker class to execute the `greet` task:

```java
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GreetingsWorker implements Worker {
    
    @Override
    public String getTaskDefName() {
        return "greet";
    }

    @Override
    public TaskResult execute(Task task) {
        String name = (String) task.getInputData().get("name");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("greeting", "Hello, " + name + "!");
        return result;
    }
}
```

### Step 3: Write _Hello World_ Application

```java
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import java.util.List;

public class HelloWorld {
    public static void main(String[] args) {
        // Point to the Conductor Server
        ConductorClient client = new ConductorClient("http://localhost:8080/api");
        
        // Create workflow executor
        WorkflowExecutor executor = new WorkflowExecutor(client);
        
        // Register the workflow
        GreetingsWorkflow greetingsWorkflow = new GreetingsWorkflow();
        executor.registerWorkflow(greetingsWorkflow.createWorkflow(), true);
        
        // Start workers in a separate thread
        TaskClient taskClient = new TaskClient(client);
        TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(
            taskClient, 
            List.of(new GreetingsWorker())
        ).withThreadCount(10).build();
        
        configurer.init();
        
        // Start workflow execution
        WorkflowClient workflowClient = new WorkflowClient(client);
        String workflowId = workflowClient.startWorkflow(
            "greetings", 
            1, 
            "", 
            Map.of("name", "World")
        );
        
        System.out.println("Started workflow: " + workflowId);
    }
}
```

## Running Workflows on Conductor Standalone (Installed Locally)

### Setup Environment Variable

```shell
export CONDUCTOR_SERVER_URL=http://localhost:8080/api
```

### Start Conductor Server

To start the Conductor server locally:

```shell
docker run --init -p 8080:8080 -p 5000:5000 conductoross/conductor-standalone:latest
```

Alternatively, you can use the [Orkes CLI](https://github.com/orkes-io/orkes-cli) to start the server:

```shell
orkes server start
```

### Execute Hello World Application

```shell
./gradlew run
```

Now, the workflow is executed, and its execution status can be viewed from the Conductor UI at http://localhost:5000.

## Running Workflows on Orkes Conductor

For running workflows on the Orkes Conductor (hosted):

1. Get a free developer account from [Orkes Cloud](https://cloud.orkes.io/)
2. Create an application with full access
3. Set environment variables:

```shell
export CONDUCTOR_SERVER_URL=https://[your-cluster].orkesconductor.io/api
export CONDUCTOR_AUTH_KEY=your-key
export CONDUCTOR_AUTH_SECRET=your-secret
```

## Learn More about Conductor Java SDK

Visit [the official documentation for the Conductor Java SDK](https://orkes.io/content).

## Writing Workers

Workers are the building blocks of Conductor workflows. They are responsible for executing tasks.

### Implementing Workers

Implement the `Worker` interface:

```java
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MyWorker implements Worker {
    
    @Override
    public String getTaskDefName() {
        return "my_task";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        // Add your business logic here
        result.addOutputData("result", "Task completed successfully");
        return result;
    }
}
```

### Managing Workers in Application

Use `TaskRunnerConfigurer` to manage workers:

```java
TaskClient taskClient = new TaskClient(conductorClient);
TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(
    taskClient,
    List.of(new MyWorker(), new AnotherWorker())
)
.withThreadCount(10)
.build();

configurer.init();
```

### Design Principles for Workers

Each worker should:
- Be stateless and idempotent
- Handle failure scenarios gracefully
- Report status back to Conductor
- Complete execution quickly (or use polling for long-running tasks)

### Worker vs. Microservice/HTTP Endpoints

Workers are lightweight, efficient alternatives to microservices for task execution:

| Feature | Worker | HTTP Endpoint |
|---------|--------|---------------|
| Deployment | Embedded in application | Separate service |
| Scalability | Horizontal (add more worker instances) | Horizontal (add more service instances) |
| Latency | Lower (direct connection) | Higher (network overhead) |
| Complexity | Simple | Complex (service mesh, load balancer) |

### Deploying Workers in Production

Best practices:
- Run multiple worker instances for high availability
- Monitor worker health and performance
- Use appropriate thread pool sizes based on workload
- Implement graceful shutdown handling

## Create Conductor Workflows

Workflows define the sequence and logic of task execution.

### Creating Workflows

#### Using Code (Workflow-as-Code):

```java
ConductorWorkflow<WorkflowInput> workflow = new ConductorWorkflow<>(executor);
workflow.setName("my_workflow");
workflow.setVersion(1);

SimpleTask task1 = new SimpleTask("task1", "task1_ref");
SimpleTask task2 = new SimpleTask("task2", "task2_ref");

workflow.add(task1);
workflow.add(task2);

executor.registerWorkflow(workflow, true);
```

For more examples, see [java-sdk/workflow_sdk.md](java-sdk/workflow_sdk.md).

### Executing Workflows

Start a workflow execution:

```java
WorkflowClient workflowClient = new WorkflowClient(conductorClient);

String workflowId = workflowClient.startWorkflow(
    "my_workflow",     // workflow name
    1,                 // version
    "",                // correlation ID (optional)
    Map.of("key", "value")  // input
);
```

### Managing Workflow Executions

**Get Execution Status:**

```java
Workflow workflow = workflowClient.getWorkflow(workflowId, true);
System.out.println("Status: " + workflow.getStatus());
```

**Terminate Running Workflows:**

```java
workflowClient.terminateWorkflow(workflowId, "Termination reason");
```

**Retry Failed Workflows:**

```java
workflowClient.retryWorkflow(workflowId);
```

**Restart Workflows:**

```java
workflowClient.restartWorkflow(workflowId, false);
```

**Pause Running Workflow:**

```java
workflowClient.pauseWorkflow(workflowId);
```

**Resume Paused Workflow:**

```java
workflowClient.resumeWorkflow(workflowId);
```

### Searching for Workflows

```java
String query = "workflowType = 'my_workflow' AND status = 'COMPLETED'";
SearchResult<WorkflowSummary> results = workflowClient.search(query);
```

### Handling Failures, Retries and Rate Limits

Configure task-level retry and rate limits in the task definition:

```java
TaskDef taskDef = new TaskDef();
taskDef.setName("my_task");
taskDef.setRetryCount(3);
taskDef.setRetryDelaySeconds(60);
taskDef.setRateLimitPerFrequency(100);
taskDef.setRateLimitFrequencyInSeconds(60);

MetadataClient metadataClient = new MetadataClient(conductorClient);
metadataClient.registerTaskDef(taskDef);
```

## Using Conductor in Your Application

Conductor SDKs are lightweight and can easily be added to your existing or new Java app.

### Adding Conductor SDK to Your Application

Conductor Java SDKs are published on Maven Central:

**For Gradle:**

```gradle
dependencies {
    implementation 'org.conductoross:conductor-client:4.2.0'
    implementation 'org.conductoross:java-sdk:4.2.0'
}
```

**For Maven:**

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>4.2.0</version>
</dependency>
```

### Testing Workflows

Conductor SDK for Java provides a testing framework for workflow-based applications. See [java-sdk/testing_framework.md](java-sdk/testing_framework.md) for details.

```java
WorkflowTestRunner testRunner = new WorkflowTestRunner(conductorClient);
WorkflowTestRequest testRequest = new WorkflowTestRequest()
    .name("my_workflow")
    .version(1)
    .input(Map.of("key", "value"));

Workflow result = testRunner.testWorkflow(testRequest);
assertEquals("COMPLETED", result.getStatus());
```

> [!note]
> Workflow workers are your regular Java functions and can be tested with any available testing framework like JUnit or TestNG.

### Workflow Deployments Using CI/CD

> [!tip]
> Treat your workflow definitions just like your code. We recommend checking the workflow definitions into version control and using CI/CD to promote workflow definitions across environments.

Best practices:
- Check in workflow and task definitions with application code
- Use `MetadataClient` to register/update workflows during deployment
- Version your workflows for canary testing and rollback capability

### Versioning Workflows

A powerful feature of Conductor is the ability to version workflows:

```java
// Register version 2 of the workflow
workflow.setVersion(2);
executor.registerWorkflow(workflow, true);

// Start specific version
workflowClient.startWorkflow("my_workflow", 2, "", input);
```

Benefits:
- Run multiple versions simultaneously
- Canary testing in production
- A/B testing across versions
- Rollback capability by deleting problematic versions

---

## Modules

The Conductor Java SDK is organized into several modules:

- **[conductor-client](conductor-client/README.md)** - Core HTTP client for interacting with Conductor
- **[java-sdk](java-sdk/README.md)** - Workflow-as-code SDK
- **[conductor-client-spring](conductor-client-spring/README.md)** - Spring Boot auto-configuration
- **[conductor-client-metrics](conductor-client-metrics/README.md)** - Metrics and monitoring
- **[examples](examples/README.md)** - Sample applications

## Roadmap

For insights into the Conductor project's future plans and upcoming features, check out the roadmap here: [Conductor OSS Roadmap](https://github.com/orgs/conductor-oss/projects/3).

## Feedback

We are building this based on feedback from our users and community. 

We encourage everyone to share their thoughts and feedback! You can create new GitHub issues or comment on existing ones. You can also join our [Slack community](https://join.slack.com/t/orkes-conductor/shared_invite/zt-2vdbx239s-Eacdyqya9giNLHfrCavfaA) to connect with us.

Thank you! ♥

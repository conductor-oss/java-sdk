package orkescloud;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.HeaderSupplier;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.run.Workflow;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Shared Conductor client utilities using conductor-oss Java SDK v5.
 *
 * Supports two connection modes:
 * - Local: connects to a local Conductor instance via CONDUCTOR_BASE_URL
 * - Cloud: connects to Orkes Conductor Cloud via CONDUCTOR_SERVER_URL with
 *   API key authentication using CONDUCTOR_KEY_ID and CONDUCTOR_KEY_SECRET
 *
 * Note: The conductor-oss SDK v5 does not have a built-in credentials() method
 * on the builder. For Orkes Cloud authentication, this helper uses
 * addHeaderSupplier() to inject X-Authorization headers with the API key/secret.
 * For production Orkes Cloud usage, consider the official Orkes Java SDK which
 * has native token-based auth support.
 */
public class ConductorClientHelper {

    private static final String DEFAULT_LOCAL_URL = "http://localhost:8080/api";

    private final ConductorClient client;
    private final MetadataClient metadataClient;
    private final WorkflowClient workflowClient;
    private final TaskClient taskClient;
    private final boolean cloudMode;
    private TaskRunnerConfigurer configurer;

    /**
     * Creates a helper connected to the local Conductor instance.
     * Uses CONDUCTOR_BASE_URL env var, defaulting to http://localhost:8080/api.
     */
    public ConductorClientHelper() {
        this(System.getenv("CONDUCTOR_BASE_URL") != null
                ? System.getenv("CONDUCTOR_BASE_URL")
                : DEFAULT_LOCAL_URL, null, null);
    }

    /**
     * Creates a helper with explicit connection parameters.
     *
     * @param serverUrl the Conductor server URL (e.g., https://play.orkes.io/api for cloud)
     * @param keyId     the Orkes Cloud API key ID (null for local mode)
     * @param keySecret the Orkes Cloud API key secret (null for local mode)
     */
    public ConductorClientHelper(String serverUrl, String keyId, String keySecret) {
        this.cloudMode = keyId != null && keySecret != null
                && !keyId.isBlank() && !keySecret.isBlank();

        var builder = ConductorClient.builder().basePath(serverUrl);

        if (cloudMode) {
            // The conductor-oss SDK v5 does not provide a credentials() method.
            // We use addHeaderSupplier() to inject auth headers for Orkes Cloud.
            // This sends the key ID and secret as custom headers that Orkes Cloud
            // uses for API key authentication.
            builder.addHeaderSupplier(new HeaderSupplier() {
                @Override
                public void init(ConductorClient client) {
                    // No initialization needed for static API keys
                }

                @Override
                public Map<String, String> get(String method, String path) {
                    return Map.of(
                            "X-Authorization", keyId + ":" + keySecret
                    );
                }
            });
        }

        this.client = builder.build();
        this.metadataClient = new MetadataClient(client);
        this.workflowClient = new WorkflowClient(client);
        this.taskClient = new TaskClient(client);
    }

    public boolean isCloudMode() {
        return cloudMode;
    }

    public void registerTaskDefs(List<String> taskNames) {
        List<TaskDef> defs = taskNames.stream().map(name -> {
            TaskDef def = new TaskDef();
            def.setName(name);
            def.setRetryCount(2);
            def.setTimeoutSeconds(60);
            def.setResponseTimeoutSeconds(30);
            def.setOwnerEmail("examples@orkes.io");
            return def;
        }).toList();
        metadataClient.registerTaskDefs(defs);
    }

    public void registerWorkflow(String resourcePath) throws Exception {
        registerWorkflow(resourcePath, null, null);
    }

    /**
     * Registers a workflow from a JSON resource, optionally overriding the
     * workflow name and the task name used in the first SIMPLE task.
     *
     * @param resourcePath  classpath resource path for the workflow JSON
     * @param workflowName  if non-null, overrides the name in the JSON
     * @param taskName      if non-null, overrides the first task's name in the JSON
     */
    public void registerWorkflow(String resourcePath, String workflowName, String taskName)
            throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            WorkflowDef workflowDef = mapper.readValue(json, WorkflowDef.class);
            if (workflowName != null) {
                workflowDef.setName(workflowName);
            }
            if (taskName != null && !workflowDef.getTasks().isEmpty()) {
                String oldRefName = workflowDef.getTasks().get(0).getTaskReferenceName();
                String newRefName = taskName + "_ref";
                workflowDef.getTasks().get(0).setName(taskName);
                workflowDef.getTasks().get(0).setTaskReferenceName(newRefName);

                // Update output parameter references to use the new task ref name
                if (oldRefName != null && workflowDef.getOutputParameters() != null) {
                    var updatedOutputParams = new java.util.HashMap<String, Object>();
                    for (var entry : workflowDef.getOutputParameters().entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof String s) {
                            updatedOutputParams.put(entry.getKey(),
                                    s.replace(oldRefName, newRefName));
                        } else {
                            updatedOutputParams.put(entry.getKey(), value);
                        }
                    }
                    workflowDef.setOutputParameters(updatedOutputParams);
                }
            }
            metadataClient.updateWorkflowDefs(List.of(workflowDef));
        }
    }

    public String startWorkflow(String name, int version, Map<String, Object> input) {
        var request = new StartWorkflowRequest();
        request.setName(name);
        request.setVersion(version);
        request.setInput(input);
        return workflowClient.startWorkflow(request);
    }

    public Workflow waitForWorkflow(String workflowId, String targetStatus, long maxWaitMs)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < maxWaitMs) {
            Workflow workflow = workflowClient.getWorkflow(workflowId, true);
            String status = workflow.getStatus().name();
            if (targetStatus.equals(status)
                    || "COMPLETED".equals(status)
                    || "FAILED".equals(status)
                    || "TERMINATED".equals(status)) {
                return workflow;
            }
            Thread.sleep(500);
        }
        return workflowClient.getWorkflow(workflowId, true);
    }

    public void startWorkers(List<Worker> workers) {
        configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
                .withThreadCount(workers.size())
                .withSleepWhenRetry(100)
                .build();
        configurer.init();
    }

    public void stopWorkers() {
        if (configurer != null) {
            configurer.shutdown();
        }
    }
}

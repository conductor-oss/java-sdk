package shoppingcart;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
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

public class ConductorClientHelper {

    private static final String CONDUCTOR_SERVER_URL =
            System.getenv("CONDUCTOR_BASE_URL") != null
                    ? System.getenv("CONDUCTOR_BASE_URL")
                    : "http://localhost:8080/api";

    private final ConductorClient client;
    private final MetadataClient metadataClient;
    private final WorkflowClient workflowClient;
    private final TaskClient taskClient;
    private TaskRunnerConfigurer configurer;

    public ConductorClientHelper() {
        this.client = ConductorClient.builder().basePath(CONDUCTOR_SERVER_URL).build();
        this.metadataClient = new MetadataClient(client);
        this.workflowClient = new WorkflowClient(client);
        this.taskClient = new TaskClient(client);
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
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            WorkflowDef workflowDef = mapper.readValue(json, WorkflowDef.class);
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
            if (targetStatus.equals(status) || "COMPLETED".equals(status)
                    || "FAILED".equals(status) || "TERMINATED".equals(status)) {
                return workflow;
            }
            Thread.sleep(500);
        }
        return workflowClient.getWorkflow(workflowId, true);
    }

    public void startWorkers(List<Worker> workers) {
        configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
                .withThreadCount(workers.size()).withSleepWhenRetry(100).build();
        configurer.init();
    }

    public void stopWorkers() {
        if (configurer != null) configurer.shutdown();
    }
}

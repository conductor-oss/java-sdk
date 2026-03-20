package datalakeingestion;

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
    private static final String CONDUCTOR_SERVER_URL = System.getenv("CONDUCTOR_BASE_URL") != null ? System.getenv("CONDUCTOR_BASE_URL") : "http://localhost:8080/api";
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
        List<TaskDef> defs = taskNames.stream().map(name -> { TaskDef d = new TaskDef(); d.setName(name); d.setRetryCount(2); d.setTimeoutSeconds(60); d.setResponseTimeoutSeconds(30); d.setOwnerEmail("examples@orkes.io"); return d; }).toList();
        metadataClient.registerTaskDefs(defs);
    }

    public void registerWorkflow(String resourcePath) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            WorkflowDef wfDef = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, WorkflowDef.class);
            metadataClient.updateWorkflowDefs(List.of(wfDef));
        }
    }

    public String startWorkflow(String name, int version, Map<String, Object> input) {
        var req = new StartWorkflowRequest(); req.setName(name); req.setVersion(version); req.setInput(input);
        return workflowClient.startWorkflow(req);
    }

    public Workflow waitForWorkflow(String workflowId, String targetStatus, long maxWaitMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < maxWaitMs) {
            Workflow wf = workflowClient.getWorkflow(workflowId, true);
            String s = wf.getStatus().name();
            if (targetStatus.equals(s) || "COMPLETED".equals(s) || "FAILED".equals(s) || "TERMINATED".equals(s)) return wf;
            Thread.sleep(500);
        }
        return workflowClient.getWorkflow(workflowId, true);
    }

    public void startWorkers(List<Worker> workers) {
        configurer = new TaskRunnerConfigurer.Builder(taskClient, workers).withThreadCount(workers.size()).withSleepWhenRetry(100).build();
        configurer.init();
    }

    public void stopWorkers() { if (configurer != null) configurer.shutdown(); }
}

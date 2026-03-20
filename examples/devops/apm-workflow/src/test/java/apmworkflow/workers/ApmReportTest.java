package apmworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ApmReportTest {

    private final ApmReport worker = new ApmReport();

    @Test
    void taskDefName() {
        assertEquals("apm_report", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("checkout-service");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsReportGenerated() {
        Task task = taskWith("checkout-service");
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("reportGenerated"));
    }

    @Test
    void returnsReportUrl() {
        Task task = taskWith("checkout-service");
        TaskResult result = worker.execute(task);
        String url = (String) result.getOutputData().get("reportUrl");
        assertTrue(url.startsWith("https://apm.example.com/report/"));
    }

    @Test
    void returnsGeneratedAt() {
        Task task = taskWith("checkout-service");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("generatedAt"));
    }

    @Test
    void handlesNullServiceName() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith("api-gateway");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("reportGenerated"));
        assertNotNull(result.getOutputData().get("reportUrl"));
        assertNotNull(result.getOutputData().get("generatedAt"));
    }

    private Task taskWith(String serviceName) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", serviceName);
        input.put("bottlenecks", List.of());
        input.put("recommendations", List.of());
        task.setInputData(input);
        return task;
    }
}

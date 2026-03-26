package emailagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeRequestWorkerTest {

    private final AnalyzeRequestWorker worker = new AnalyzeRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("ea_analyze_request", worker.getTaskDefName());
    }

    @Test
    void returnsProjectUpdateEmailType() {
        Task task = taskWith(Map.of(
                "intent", "Send a project update",
                "recipient", "sarah@company.com",
                "context", "Project Alpha team lead"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("project_update", result.getOutputData().get("emailType"));
    }

    @Test
    void returnsFourKeyPoints() {
        Task task = taskWith(Map.of("intent", "Send a project update"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> keyPoints = (List<String>) result.getOutputData().get("keyPoints");
        assertNotNull(keyPoints);
        assertEquals(4, keyPoints.size());
    }

    @Test
    void returnsNormalUrgency() {
        Task task = taskWith(Map.of("intent", "Update about milestone"));
        TaskResult result = worker.execute(task);

        assertEquals("normal", result.getOutputData().get("urgency"));
    }

    @Test
    void returnsProfessionalFormality() {
        Task task = taskWith(Map.of("intent", "Project status update"));
        TaskResult result = worker.execute(task);

        assertEquals("professional", result.getOutputData().get("formality"));
    }

    @Test
    void handlesNullIntent() {
        Map<String, Object> input = new HashMap<>();
        input.put("intent", null);
        input.put("recipient", "sarah@company.com");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("emailType"));
    }

    @Test
    void handlesBlankRecipient() {
        Task task = taskWith(Map.of("intent", "Send update", "recipient", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("keyPoints"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("emailType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}

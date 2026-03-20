package authenticationworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IssueTokenWorkerTest {

    private final IssueTokenWorker worker = new IssueTokenWorker();

    @Test
    void taskDefName() {
        assertEquals("auth_issue_token", worker.getTaskDefName());
    }

    @Test
    void issuesTokenSuccessfully() {
        Task task = taskWith(Map.of("issue_tokenData", Map.of("risk_assessment", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("issue_token"));
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void outputContainsIssueToken() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("issue_token"));
    }

    @Test
    void outputContainsCompletedAt() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("completedAt"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesExtraInputFields() {
        Task task = taskWith(Map.of("extra", "data"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void deterministicOutput() {
        Task task1 = taskWith(Map.of());
        Task task2 = taskWith(Map.of());
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("issue_token"), r2.getOutputData().get("issue_token"));
        assertEquals(r1.getOutputData().get("completedAt"), r2.getOutputData().get("completedAt"));
    }

    @Test
    void completedAtFormat() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        String completedAt = (String) result.getOutputData().get("completedAt");
        assertTrue(completedAt.contains("T"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}

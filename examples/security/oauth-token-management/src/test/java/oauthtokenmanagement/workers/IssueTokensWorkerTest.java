package oauthtokenmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IssueTokensWorkerTest {

    private final IssueTokensWorker worker = new IssueTokensWorker();

    @Test void taskDefName() { assertEquals("otm_issue_tokens", worker.getTaskDefName()); }

    @Test void issuesTokensSuccessfully() {
        Task task = taskWith(Map.of("scope", "read:users write:reports"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("issue_tokens"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test void outputContainsIssueTokens() {
        assertEquals(true, worker.execute(taskWith(Map.of())).getOutputData().get("issue_tokens"));
    }

    @Test void outputContainsProcessed() {
        assertEquals(true, worker.execute(taskWith(Map.of())).getOutputData().get("processed"));
    }

    @Test void handlesNullScope() {
        Map<String, Object> input = new HashMap<>(); input.put("scope", null);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(input)).getStatus());
    }

    @Test void handlesMissingInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    @Test void handlesEmptyScope() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of("scope", ""))).getStatus());
    }

    @Test void deterministicOutput() {
        Task t1 = taskWith(Map.of("scope", "read")); Task t2 = taskWith(Map.of("scope", "read"));
        assertEquals(worker.execute(t1).getOutputData(), worker.execute(t2).getOutputData());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input)); return task;
    }
}

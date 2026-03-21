package emailapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessDecisionWorkerTest {

    @Test
    void taskDefName() {
        ProcessDecisionWorker worker = new ProcessDecisionWorker();
        assertEquals("ea_process_decision", worker.getTaskDefName());
    }

    @Test
    void returnsProcessedTrueWithApproveDecision() {
        ProcessDecisionWorker worker = new ProcessDecisionWorker();
        Task task = taskWith(new HashMap<>(Map.of("decision", "approve")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void returnsProcessedTrueWithRejectDecision() {
        ProcessDecisionWorker worker = new ProcessDecisionWorker();
        Task task = taskWith(new HashMap<>(Map.of("decision", "reject")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNoDecisionInput() {
        ProcessDecisionWorker worker = new ProcessDecisionWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsProcessedKey() {
        ProcessDecisionWorker worker = new ProcessDecisionWorker();
        Task task = taskWith(new HashMap<>(Map.of("decision", "approve")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("processed"));
    }

    @Test
    void deterministicOutputForSameInput() {
        ProcessDecisionWorker worker = new ProcessDecisionWorker();

        Task task1 = taskWith(new HashMap<>(Map.of("decision", "approve")));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(new HashMap<>(Map.of("decision", "approve")));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("processed"), result2.getOutputData().get("processed"));
    }

    @Test
    void handlesNullDecisionGracefully() {
        ProcessDecisionWorker worker = new ProcessDecisionWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("decision", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}

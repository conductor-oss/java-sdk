package waitrestapi.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandleDecisionWorkerTest {

    @Test
    void taskDefName() {
        HandleDecisionWorker worker = new HandleDecisionWorker();
        assertEquals("wapi_handle_decision", worker.getTaskDefName());
    }

    @Test
    void handlesApprovedDecision() {
        HandleDecisionWorker worker = new HandleDecisionWorker();
        Task task = taskWith(Map.of("decision", "approved"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("handled-approved", result.getOutputData().get("result"));
    }

    @Test
    void handlesRejectedDecision() {
        HandleDecisionWorker worker = new HandleDecisionWorker();
        Task task = taskWith(Map.of("decision", "rejected"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("handled-rejected", result.getOutputData().get("result"));
    }

    @Test
    void handlesUnknownDecisionGracefully() {
        HandleDecisionWorker worker = new HandleDecisionWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("handled-unknown", result.getOutputData().get("result"));
    }

    @Test
    void handlesNonStringDecisionGracefully() {
        HandleDecisionWorker worker = new HandleDecisionWorker();
        Task task = taskWith(Map.of("decision", 123));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("handled-unknown", result.getOutputData().get("result"));
    }

    @Test
    void outputIsDeterministic() {
        HandleDecisionWorker worker = new HandleDecisionWorker();

        Task task1 = taskWith(Map.of("decision", "approved"));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("decision", "approved"));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("result"), result2.getOutputData().get("result"));
        assertEquals(result1.getStatus(), result2.getStatus());
    }

    @Test
    void handlesCustomDecisionValues() {
        HandleDecisionWorker worker = new HandleDecisionWorker();
        Task task = taskWith(Map.of("decision", "escalated"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("handled-escalated", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}

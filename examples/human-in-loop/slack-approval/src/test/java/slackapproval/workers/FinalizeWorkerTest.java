package slackapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    @Test
    void taskDefName() {
        FinalizeWorker worker = new FinalizeWorker();
        assertEquals("sa_finalize", worker.getTaskDefName());
    }

    @Test
    void returnsDoneTrueWithApprovedDecision() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(new HashMap<>(Map.of("decision", "approved")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
        assertEquals("approved", result.getOutputData().get("decision"));
    }

    @Test
    void returnsDoneTrueWithRejectedDecision() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(new HashMap<>(Map.of("decision", "rejected")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
        assertEquals("rejected", result.getOutputData().get("decision"));
    }

    @Test
    void usesDefaultDecisionWhenNotProvided() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("done"));
        assertEquals("unknown", result.getOutputData().get("decision"));
    }

    @Test
    void outputContainsDoneKey() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(new HashMap<>(Map.of("decision", "approved")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("done"));
        assertTrue(result.getOutputData().containsKey("decision"));
    }

    @Test
    void handlesEmptyStringDecision() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(new HashMap<>(Map.of("decision", "")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("decision"));
    }

    @Test
    void handlesNonStringDecisionGracefully() {
        FinalizeWorker worker = new FinalizeWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("decision", 42);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("decision"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
